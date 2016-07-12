package au.csiro.data61.gnaf.contrib.service

import java.io.IOException

import scala.concurrent.{ ExecutionContextExecutor, Future }

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.{ sprayJsonMarshaller, sprayJsonUnmarshaller }
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directive._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.http.scaladsl.server.directives.LoggingMagnet._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import au.csiro.data61.gnaf.common.util.Util
import au.csiro.data61.gnaf.contrib.db.ContribTables
import ch.megard.akka.http.cors.CorsDirectives.cors
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import ch.megard.akka.http.cors.CorsSettings
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods
import slick.dbio.DBIOAction

//import io.swagger.annotations._
//import javax.ws.rs.{ Consumes, DELETE, GET, POST, PUT, Path, PathParam, Produces, WebApplicationException }

// // next two also needed in gnaf-service so maybe move them all to gnaf-common?
// case class GeocodeType(code: String, description: String)
// case class GeocodeTypes(types: Seq[GeocodeType])
case class ContribGeocode(id: Option[Long], contribStatus: String, addressSiteGeocodePid: Option[String], dateCreated: Long, version: Int, addressSitePid: String, geocodeTypeCode: String, longitude: scala.math.BigDecimal, latitude: scala.math.BigDecimal)
case class ContribGeocodeKey(id: Long, version: Int)

trait Protocols extends DefaultJsonProtocol {
//  implicit val geocodeTypeFormat = jsonFormat2(GeocodeType.apply)
//  implicit val geocodeTypesFormat = jsonFormat1(GeocodeTypes.apply)
  implicit val contribGeocodeFormat = jsonFormat9(ContribGeocode.apply)
  implicit val contribGeocodeKeyFormat = jsonFormat2(ContribGeocodeKey.apply)
}

//@Path("/")  // Swagger
//@Api(value = "/", produces = "application/json")
trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  val config = ConfigFactory.load
  val logger: LoggingAdapter
  
//  // how to call other services, not needed here
//  
//  // make HTTP JSON request to gnafService
//  lazy val gnafServiceFlow: Flow[HttpRequest, HttpResponse, Any] = Http().outgoingConnection(config.getString("services.gnafService.interface"), config.getInt("services.gnafService.port"))
//  def gnafServiceRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(gnafServiceFlow).runWith(Sink.head)
//  
//  def geocodeTypes: Future[GeocodeTypes] = {
//    gnafServiceRequest(RequestBuilding.Get("/geocodeTypes")).flatMap { response =>
//      response.status match {
//        case OK => Unmarshal(response.entity).to[GeocodeTypes]
//        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
//          val error = s"GnafService request failed with status code ${response.status} and entity $entity"
//          logger.error(error)
//          Future.failed(new IOException(error))
//        }
//      }
//    }
//  }
  
  object MyContribTables extends {
    val profile = Util.getObject[slick.driver.JdbcProfile](config.getString("gnafContribDb.slickDriver")) // e.g. slick.driver.{H2Driver,PostgresDriver}
  } with ContribTables
  import MyContribTables._
  import MyContribTables.profile.api._
  
  implicit val db = Database.forConfig("gnafContribDb", config)
    
  def createSchemaIfNotExists = {
    import slick.jdbc.ResultSetAction
    import slick.jdbc.GetResult._
    import scala.concurrent.Await
    import scala.concurrent.duration._
    
    val listTablesAction = ResultSetAction[(String, String, String, String)](_.conn.getMetaData.getTables("", "", null, null)).map(_.filter(_._4 == "TABLE").map(_._3))
    val createIfNotExistsAction = listTablesAction.flatMap { tbls => 
      if (tbls.isEmpty) schema.create.map(_ => "createSchemaIfNotExists: schema created")
      else DBIOAction.successful(s"createSchemaIfNotExists: pre-existing tables = $tbls") 
    }
    logger.info(Await.result(db.run(createIfNotExistsAction), 15.seconds))
  }
  
  val qList = {
    def q(addressSitePid: Rep[String]) = AddressSiteGeocode.filter(_.addressSitePid === addressSitePid)
    Compiled(q _)
  }
  def list(addressSitePid: String): Future[Seq[ContribGeocode]] = db.run(qList(addressSitePid).result).map(_.map(toContribGeocode))
  def toContribGeocode(x: AddressSiteGeocodeRow) = ContribGeocode(x.id, x.contribStatus, x.addressSiteGeocodePid, x.dateCreated.getTime, x.version, x.addressSitePid, x.geocodeTypeCode, x.longitude, x.latitude)
  
  val contribGeocodeWithId = (AddressSiteGeocode returning AddressSiteGeocode.map(_.id) )
  def toAddressSiteGeocodeRow(x: ContribGeocode) = AddressSiteGeocodeRow(x.id, x.contribStatus, x.addressSiteGeocodePid, new java.sql.Date(x.dateCreated), x.version, x.addressSitePid, x.geocodeTypeCode, x.longitude, x.latitude)
  
  def createContrib(c: ContribGeocode) = {
    val c2: ContribGeocode = (c.copy(dateCreated = System.currentTimeMillis, version = 1))
    db.run(contribGeocodeWithId += toAddressSiteGeocodeRow(c2)).map(id => c2.copy(id = Some(id)))
  }
  
  def qGet = {
    def q(id: Rep[Long], version: Rep[Int]) = AddressSiteGeocode.filter(x => x.id === id && x.version === version)
    Compiled(q _)
  }
  def deleteContrib(key: ContribGeocodeKey) = db.run(qGet(key.id, key.version).delete)
  
  def updateContrib(c: ContribGeocode) = {
    val c2 = c.copy(version = c.version + 1, dateCreated = System.currentTimeMillis)
    db.run(qGet(c.id.get, c.version).update(toAddressSiteGeocodeRow(c2))).map(cnt => if (cnt == 1) Some(c2) else None)
  }
  
  val settings = CorsSettings.defaultSettings.copy(allowedMethods = HttpMethods.DELETE +: CorsSettings.defaultSettings.allowedMethods)
  val routes = {
    logRequestResult("GnafContrib") {  cors(settings) {
//      pathPrefix("geocodeTypes") {
//        get {
//          complete {
//            geocodeTypes
//          }
//        }
//      } ~
      pathPrefix("contrib") {
        (post & entity(as[ContribGeocode])) { contribGeocode =>
          logger.info(s"POST contrib: contribGeocode = $contribGeocode")
          complete {
            createContrib(contribGeocode)
          }
        } ~
        (get & path(Segment)) { addressSitePid =>
          complete {
            list(addressSitePid)
          }
        } ~
        (delete & entity(as[ContribGeocodeKey])) { key =>
          complete {
            deleteContrib(key).map[ToResponseMarshallable] { cnt =>
              if (cnt == 1) key else (BadRequest -> s"key = $key not found")
            }
          }
        } ~
        (put & entity(as[ContribGeocode])) { c =>
          complete {
            updateContrib(c).map[ToResponseMarshallable] { x => if (x.isDefined) x.get else BadRequest -> s"id = ${c.id}, version = ${c.version} not found" }
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
    createSchemaIfNotExists
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
