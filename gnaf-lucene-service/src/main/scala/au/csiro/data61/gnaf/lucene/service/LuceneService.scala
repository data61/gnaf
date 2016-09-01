package au.csiro.data61.gnaf.lucene.service

import java.io.File

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.reflect.runtime.universe

import org.apache.lucene.document.Document
import org.apache.lucene.search.{ ScoreDoc, Sort }

import com.github.swagger.akka.{ HasActorSystem, SwaggerHttpService }
import com.github.swagger.akka.model.Info
import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.{ sprayJsonMarshaller, sprayJsonUnmarshaller }
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server.directives.LoggingMagnet.forRequestResponseFromMarker
import akka.stream.{ ActorMaterializer, Materializer }
import au.csiro.data61.gnaf.lucene.util.GnafLucene._
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.{ Searcher, directory }
import au.csiro.data61.gnaf.util.Util.getLogger
import ch.megard.akka.http.cors.CorsDirectives.cors
import io.swagger.annotations.{ Api, ApiOperation, ApiParam }
import javax.ws.rs.Path
import spray.json.DefaultJsonProtocol

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
  }
  
  case class Hit(score: Float, json: String, d61Address: List[String], d61AddressNoAlias: String)
  def toHit(scoreDoc: ScoreDoc, doc: Document) = {
    Hit(scoreDoc.score, doc.get(F_JSON), doc.getValues(F_D61ADDRESS).toList, doc.get(F_D61ADDRESS_NOALIAS))
  }
  
  case class Result(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
  def toResult(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
    = Result(totalHits, elapsedSecs, hits, error)
  
  def toSort(f: Option[String], asc: Boolean): Option[Sort] = None
  
  def validationBuf(c: CliOption, qp: QueryParam): ListBuffer[String] = {
    val b = new ListBuffer[String]()
    if (qp.numHits > c.numHits) b += s"numHits = ${qp.numHits} exceeds max of ${c.numHits}"
    qp.fuzzy.foreach { f =>
      if (f.minLength < c.minFuzzyLength) b += s"fuzzy minLength = ${f.minLength} less than min of ${c.minFuzzyLength}"
      if (f.maxEdits > c.fuzzyMaxEdits) b += s"fuzzy maxEdits = ${f.maxEdits} exceeds max of ${c.fuzzyMaxEdits}"
      if (f.prefixLength < c.fuzzyPrefixLength) b += s"fuzzy prefixLength = ${f.prefixLength} less than min of ${c.fuzzyPrefixLength}"
      if (f.prefixLength >= f.minLength) b += s"fuzzy prefixLength = ${f.prefixLength} not less than minLength = ${f.minLength}"
    }
    b
  }
  
  /** validation error message or empty for no error */
  def validationError(b: ListBuffer[String]) = b.mkString("\n")
  
  case class BulkQueryParam(addresses: Seq[String], numHits: Int, fuzzy: Option[FuzzyParam], box: Option[BoundingBox])

  def validationBuf(c: CliOption, bqp: BulkQueryParam): ListBuffer[String] = {
    val b = validationBuf(c, QueryParam("", bqp.numHits, bqp.fuzzy, bqp.box))
    if (bqp.addresses.size > c.bulk) b += s"addresses.size = ${bqp.addresses.size} exceeds max of ${c.bulk}"
    b
  }
  
  object JsonProtocol extends DefaultJsonProtocol {
    implicit val hitFormat = jsonFormat4(Hit)
    implicit val resultFormat = jsonFormat4(Result)
    implicit val fuzzyParamFormat = jsonFormat3(FuzzyParam)
    implicit val boundingBoxFormat = jsonFormat4(BoundingBox)
    implicit val queryParamFormat = jsonFormat4(QueryParam)
    implicit val bulkQueryParamFormat = jsonFormat4(BulkQueryParam)
  }
  
  def mkSearcher(c: CliOption) = {
    val s = new Searcher(directory(c.indexDir), toHit, toResult)
    s.searcher.setSimilarity(AddressSimilarity)
    s
  }
  
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

/*
 * Stuff that can get wiped out by Eclipse organize imports:
import LuceneService._
import LuceneService.JsonProtocol._

@Api(value = "lucene", produces = "application/json")
 */

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
    val err = validationError(validationBuf(c, q))
    validate(err.isEmpty, err) { complete { Future { 
      searcher.search(q.toQuery, q.numHits)
    }}}
  }
  
  @Path("bulkSearch")
  @ApiOperation(value = "Search for many addresses", nickname = "bulkSearch", notes="""longer description""", httpMethod = "POST", response = classOf[Seq[Result]])
  def bulkSearchRoute(
    @ApiParam(value = "bulkQueryParam", required = true) q: BulkQueryParam
  ) = {
    val err = validationError(validationBuf(c, q))
    validate(err.isEmpty, err) { complete { Future {
      def seqop(z: Seq[Result], addr: String) = z :+ searcher.search(QueryParam(addr, q.numHits, q.fuzzy, q.box).toQuery, q.numHits)
      q.addresses.par.aggregate(Seq.empty[Result])(seqop, _ ++ _)
    }}}
  }
  
  val routes = pathPrefix("lucene") {
    pathPrefix("search")     { (post & entity(as[QueryParam]))     { searchRoute     } } ~
    pathPrefix("bulkSearch") { (post & entity(as[BulkQueryParam])) { bulkSearchRoute } }
  }

}

