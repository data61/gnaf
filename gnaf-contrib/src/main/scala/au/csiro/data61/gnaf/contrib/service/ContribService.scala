package au.csiro.data61.gnaf.contrib.service

import java.io.IOException

import scala.{ Left, Right }
import scala.concurrent.{ ExecutionContextExecutor, Future }

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.{ sprayJsonMarshaller, sprayJsonUnmarshaller }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.model.StatusCodes.{ BadRequest, OK }
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directives.{ complete, get, logRequestResult, pathPrefix, segmentStringToPathMatcher }
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server.directives.LoggingMagnet.forRequestResponseFromMarker
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import au.csiro.data61.gnaf.contrib.db.ContribTables
import ch.megard.akka.http.cors.CorsDirectives.cors
import spray.json.DefaultJsonProtocol
import au.csiro.data61.gnaf.common.util.Util

// TODO: next two also needed in gnaf-service so maybe move them all to gnaf-common?
case class GeocodeType(code: String, description: String)
case class GeocodeTypes(types: Seq[GeocodeType])

trait Protocols extends DefaultJsonProtocol {
  implicit val geocodeTypeFormat = jsonFormat2(GeocodeType.apply)
  implicit val geocodeTypesFormat = jsonFormat1(GeocodeTypes.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  val config = ConfigFactory.load
  val logger: LoggingAdapter
  
  object MyContribTables extends {
    val profile = Util.getObject[slick.driver.JdbcProfile](config.getString("gnafContribDb.slickDriver")) // e.g. slick.driver.{H2Driver,PostgresDriver}
  } with ContribTables
  val contribTables = MyContribTables
  import contribTables._
  import contribTables.profile.api._
  
  implicit val db = Database.forConfig("gnafContribDb", config)
    
  lazy val gnafServiceFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(config.getString("services.gnafService.host"), config.getInt("services.gnafService.port"))

  def gnafServiceRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(gnafServiceFlow).runWith(Sink.head)
  
  def geocodeTypes: Future[GeocodeTypes] = {
    gnafServiceRequest(RequestBuilding.Get("/geocodeTypes")).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[GeocodeTypes]
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"GnafService request failed with status code ${response.status} and entity $entity"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }

  val routes = {
    logRequestResult("GnafContrib") { cors() {
      pathPrefix("geocodeTypes") {
        get {
          complete {
            geocodeTypes
          }
        }
      }
    } }
  }

}

object ContribService extends Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val logger = Logging(system, getClass)
  
  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
