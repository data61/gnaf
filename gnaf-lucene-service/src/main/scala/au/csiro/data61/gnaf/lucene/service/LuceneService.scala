package au.csiro.data61.gnaf.lucene.service

import java.io.File

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io.Source
import scala.reflect.runtime.universe

import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, BoostQuery, FuzzyQuery, Query, Sort, TermQuery }
import org.apache.lucene.search.BooleanClause.Occur.SHOULD

import com.github.swagger.akka.{ HasActorSystem, SwaggerHttpService }
import com.github.swagger.akka.model.Info
import com.typesafe.config.{ Config, ConfigFactory }

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.{ sprayJsonMarshaller, sprayJsonUnmarshaller }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server.directives.LoggingMagnet.forRequestResponseFromMarker
import akka.stream.{ ActorMaterializer, Materializer }
import au.csiro.data61.gnaf.lucene.util.GnafLucene.{ AddressSimilarity, F_D61ADDRESS, F_D61ADDRESS_NOALIAS, F_JSON, analyzer, shingleSize }
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.Searching.Searcher
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.tokenIter
import au.csiro.data61.gnaf.util.Util
import au.csiro.data61.gnaf.util.Util.getLogger
import ch.megard.akka.http.cors.CorsDirectives.cors
import ch.megard.akka.http.cors.CorsSettings.defaultSettings
import io.swagger.annotations.{ Api, ApiOperation, ApiParam }
import javax.ws.rs.Path
import resource.managed
import spray.json.{ DefaultJsonProtocol, pimpAny }
import scala.collection.mutable.ListBuffer

object LuceneService {
  val log = getLogger(getClass)

  case class CliOption(indexDir: File, bulk: Int, numHits: Int, minFuzzyLength: Int, fuzzyMaxEdits: Int, fuzzyPrefixLength: Int, interface: String, port: Int)
  val defaultCliOption = {
    val c = ConfigFactory.load.getConfig("gnafLuceneService")
    def gs(n: String) = c.getString(n)
    def gi(n: String) = c.getInt(n)
    CliOption(new File(gs("indexDir")), gi("bulk"), gi("numHits"), gi("minFuzzyLength"), gi("fuzzyMaxEdits"), gi("fuzzyPrefixLength"), gs("interface"), gi("port"))
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[CliOption]("gnaf-indexer") {
      head("gnaf-lucene-service", "0.x")
      note("JSON web service for address searches")
      opt[File]('i', "indexDir") action { (x, c) =>
        c.copy(indexDir = x)
      } text (s"Lucene index directory, default ${defaultCliOption.indexDir}")
      opt[Int]('b', "bulk") action { (x, c) =>
        c.copy(bulk = x)
      } text (s"max addresses client may put in a bulk request, default ${defaultCliOption.bulk}")
      opt[Int]('h', "numHits") action { (x, c) =>
        c.copy(numHits = x)
      } text (s"max client may request for the number of search hits, default ${defaultCliOption.numHits}")
      opt[Int]('f', "minFuzzyLength") action { (x, c) =>
        c.copy(minFuzzyLength = x)
      } text (s"min client may request for min query term length for fuzzy match, default ${defaultCliOption.minFuzzyLength}")
      opt[Int]('e', "fuzzyMaxEdits") action { (x, c) =>
        c.copy(fuzzyMaxEdits = x)
      } text (s"max client may request for max edits for a fuzzy match, default ${defaultCliOption.fuzzyMaxEdits}")
      opt[Int]('p', "fuzzyPrefixLength") action { (x, c) =>
        c.copy(fuzzyPrefixLength = x)
      } text (s"min client may request for min initial chars that must match exactly for a fuzzy match, default ${defaultCliOption.fuzzyPrefixLength}")
      opt[String]('n', "interface") action { (x, c) =>
        c.copy(interface = x)
      } text (s"network interface (name or IP address) to attach to, default ${defaultCliOption.interface}")
      opt[Int]('r', "port") action { (x, c) =>
        c.copy(port = x)
      } text (s"IP port to listen on, default ${defaultCliOption.port}")
      help("help") text ("prints this usage text")
    }
    parser.parse(args, defaultCliOption) foreach run
    log.info("done")
  }
  
  val fieldToLoad = Set(F_JSON, F_D61ADDRESS, F_D61ADDRESS_NOALIAS)
  
  case class Hit(score: Float, json: String, d61Address: List[String], d61AddressNoAlias: String)
  def toHit(score: Float, doc: Document) = {
    Hit(score, doc.get(F_JSON), doc.getValues(F_D61ADDRESS).toList, doc.get(F_D61ADDRESS_NOALIAS))
  }
  
  case class Result(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
  def toResult(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
    = Result(totalHits, elapsedSecs, hits, error)
  
  def toSort(f: Option[String], asc: Boolean): Option[Sort] = None
  
  case class QueryParam(addr: String, numHits: Int, minFuzzyLength: Int, fuzzyMaxEdits: Int, fuzzyPrefixLength: Int) {
    def validationBuf(c: CliOption) = {
      val b = new ListBuffer[String]()
      if (numHits > c.numHits) b += s"numHits = $numHits exceeds max of ${c.numHits}"
      if (fuzzyMaxEdits > 0) {
        if (minFuzzyLength < c.minFuzzyLength) b += s"minFuzzyLength = $minFuzzyLength less than min of ${c.minFuzzyLength}"
        if (fuzzyMaxEdits > c.fuzzyMaxEdits) b += s"fuzzyMaxEdits = $fuzzyMaxEdits exceeds max of ${c.fuzzyMaxEdits}"
        if (fuzzyPrefixLength < c.fuzzyPrefixLength) b += s"fuzzyPrefixLength = $fuzzyPrefixLength less than min of ${c.fuzzyPrefixLength}"
        if (fuzzyPrefixLength >= minFuzzyLength) b += s"fuzzyPrefixLength = $fuzzyPrefixLength not less than minFuzzyLength = $minFuzzyLength"
      }
      b
    }
    /** validation error message or empty for no error */
    def validationError(c: CliOption) = validationBuf(c).mkString("\n")
  }
  
  case class BulkQueryParam(addresses: Seq[String], numHits: Int, minFuzzyLength: Int, fuzzyMaxEdits: Int, fuzzyPrefixLength: Int) {
    def validationBuf(c: CliOption) = {
      val b = QueryParam("", numHits, minFuzzyLength, fuzzyMaxEdits, fuzzyPrefixLength).validationBuf(c)
      if (addresses.size > c.bulk) b += s"addresses.size = ${addresses.size} exceeds max of ${c.bulk}"
      b
    }
    /** validation error message or empty for no error */
    def validationError(c: CliOption) = validationBuf(c).mkString("\n")
  }
  
  object JsonProtocol extends DefaultJsonProtocol {
    implicit val hitFormat = jsonFormat4(Hit)
    implicit val resultFormat = jsonFormat4(Result)
    implicit val queryParamFormat = jsonFormat5(QueryParam)
    implicit val bulkQueryParamFormat = jsonFormat5(BulkQueryParam)
  }
  
  def mkSearcher(c: CliOption) = {
    val s = new Searcher(c.indexDir, toHit, toResult, toSort)
    s.searcher.setSimilarity(new AddressSimilarity)
    s
  }
  
  def mkQuery(c: QueryParam): Query =
    tokenIter(analyzer, F_D61ADDRESS, c.addr).foldLeft(new BooleanQuery.Builder){ (b, t) =>
      val q = {
        val term = new Term(F_D61ADDRESS, t)
        val q = if (c.fuzzyMaxEdits > 0 && t.length >= c.minFuzzyLength) new FuzzyQuery(term, c.fuzzyMaxEdits, c.fuzzyPrefixLength) else new TermQuery(term)
        val n = shingleSize(t)
        if (n < 2) q else new BoostQuery(q, Math.pow(3.0, n).toFloat)
      }
      b.add(new BooleanClause(q, SHOULD))
      b
    }.build

  def run(c: CliOption) = {
    implicit val sys = ActorSystem()
    implicit val exec = sys.dispatcher
    implicit val mat = ActorMaterializer()
    
    val luceneService = new LuceneService(c, mkSearcher(c))
    
    // serves: /api-docs/swagger.json
    val swaggerService = new SwaggerHttpService() with HasActorSystem {
      import scala.reflect.runtime.{ universe => ru }
  
      override implicit val actorSystem = sys
      override implicit val materializer = mat
      override val apiTypes = Seq(ru.typeOf[LuceneService])
      override val host = c.interface + ":" + c.port
      override val info = Info(version = "1.0")
    }
    
    val routes = cors() {
      logRequestResult("LuceneService") { luceneService.routes } ~ 
      logRequestResult("Swagger") { swaggerService.routes }
    }
    
    log.info("starting service ...")
    Http().bindAndHandle(routes, c.interface, c.port)
  }
    
}
import LuceneService._
import LuceneService.JsonProtocol._

@Api(value = "lucene", produces = "application/json")
@Path("lucene")
class LuceneService(c: CliOption, searcher: Searcher[Hit, Result])
(implicit system: ActorSystem, executor: ExecutionContextExecutor, materializer: Materializer) {
  
  @Path("search")
  @ApiOperation(value = "Search for an address", nickname = "search", notes="""longer description""", httpMethod = "POST", response = classOf[Result])
  def searchRoute(
    @ApiParam(value = "queryParam", required = true) q: QueryParam
  ) = {
    val err = q.validationError(c)
    log.debug(s"searchRoute: err = $err")
    validate(err.isEmpty, err) { complete { Future { 
      searcher.search(mkQuery(q), None, q.numHits, 0)
    }}}
  }
  
  @Path("bulkSearch")
  @ApiOperation(value = "Search for many addresses", nickname = "bulkSearch", notes="""longer description""", httpMethod = "POST", response = classOf[Seq[Result]])
  def bulkSearchRoute(
    @ApiParam(value = "bulkQueryParam", required = true) q: BulkQueryParam
  ) = {
    val err = q.validationError(c)
    validate(err.isEmpty, err) { complete { Future {
      def seqop(z: Seq[Result], addr: String) = searcher.search(mkQuery(QueryParam(addr, q.numHits, q.minFuzzyLength, q.fuzzyMaxEdits, q.fuzzyPrefixLength)), None, q.numHits, 0) +: z
      q.addresses.par.aggregate(Seq.empty[Result])(seqop, _ ++ _)
    }}}
  }
  
  val routes = pathPrefix("lucene") {
    pathPrefix("search")     { (post & entity(as[QueryParam]))     { searchRoute     } } ~
    pathPrefix("bulkSearch") { (post & entity(as[BulkQueryParam])) { bulkSearchRoute } }
  }

}

