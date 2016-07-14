package au.csiro.data61.gnaf.contrib.service

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

import com.github.swagger.akka.{ HasActorSystem, SwaggerHttpService }
import com.github.swagger.akka.model.Info
import com.typesafe.config.{ Config, ConfigFactory }

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.{ sprayJsonMarshaller, sprayJsonUnmarshaller }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives._
import akka.stream.{ ActorMaterializer, Materializer }
import au.csiro.data61.gnaf.common.util.Util
import au.csiro.data61.gnaf.contrib.db.ContribTables
import ch.megard.akka.http.cors.CorsDirectives.cors
import ch.megard.akka.http.cors.CorsSettings.defaultSettings
import io.swagger.annotations.{ Api, ApiImplicitParams, ApiImplicitParam, ApiOperation }
import javax.ws.rs.Path
import slick.dbio.DBIOAction
import slick.jdbc.ResultSetAction
import spray.json.DefaultJsonProtocol

case class ContribGeocode(id: Option[Long], contribStatus: String, addressSiteGeocodePid: Option[String], dateCreated: Long, version: Int, addressSitePid: String, geocodeTypeCode: String, longitude: scala.math.BigDecimal, latitude: scala.math.BigDecimal)
case class ContribGeocodeKey(id: Long, version: Int)

trait Protocols extends DefaultJsonProtocol {
  implicit val contribGeocodeFormat = jsonFormat9(ContribGeocode.apply)
  implicit val contribGeocodeKeyFormat = jsonFormat2(ContribGeocodeKey.apply)
}

@Path("/contrib")
@Api(value = "/contrib", produces = "application/json")
class ContribService(logger: LoggingAdapter, config: Config)(implicit system: ActorSystem, executor: ExecutionContextExecutor, materializer: Materializer) extends Protocols {
  object MyContribTables extends {
    val profile = Util.getObject[slick.driver.JdbcProfile](config.getString("gnafContribDb.slickDriver")) // e.g. slick.driver.{H2Driver,PostgresDriver}
  } with ContribTables
  import MyContribTables._
  import MyContribTables.profile.api._
  
  implicit val db = Database.forConfig("gnafContribDb", config)
    
  def createSchemaIfNotExists = {
    import scala.concurrent.Await
    import scala.concurrent.duration._
    import slick.jdbc.GetResult._
    import slick.jdbc.ResultSetAction
    
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
  
  def toContribGeocode(x: AddressSiteGeocodeRow) = ContribGeocode(x.id, x.contribStatus, x.addressSiteGeocodePid, x.dateCreated.getTime, x.version, x.addressSitePid, x.geocodeTypeCode, x.longitude, x.latitude)

  @ApiOperation(value = "List contributed geocodes for an addressSitePid", nickname = "list", httpMethod = "GET", response = classOf[ContribGeocode], responseContainer = "List")
  def listRoute(addressSitePid: String) = {
    val f = db.run(qList(addressSitePid).result).map(_.map(toContribGeocode))
    complete { f }
  }

  val contribGeocodeWithId = (AddressSiteGeocode returning AddressSiteGeocode.map(_.id) )
  def toAddressSiteGeocodeRow(x: ContribGeocode) = AddressSiteGeocodeRow(x.id, x.contribStatus, x.addressSiteGeocodePid, new java.sql.Date(x.dateCreated), x.version, x.addressSitePid, x.geocodeTypeCode, x.longitude, x.latitude)
  
  @ApiOperation(value = "Add a new contributed geocode for an addressSitePid", nickname = "create", httpMethod = "POST", response = classOf[ContribGeocode])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "c", dataType = "au.csiro.data61.gnaf.contrib.service.ContribGeocode", paramType = "body", required = true)
  ))
  def createContribRoute(c: ContribGeocode) = {
    val c2 = (c.copy(dateCreated = System.currentTimeMillis, version = 1))
    val f = db.run(contribGeocodeWithId += toAddressSiteGeocodeRow(c2)).map(id => c2.copy(id = Some(id)))
    complete { f }
  }
  
  def qGet = {
    def q(id: Rep[Long], version: Rep[Int]) = AddressSiteGeocode.filter(x => x.id === id && x.version === version)
    Compiled(q _)
  }
  
  @ApiOperation(value = "Delete a contributed geocode for an addressSitePid", nickname = "delete", httpMethod = "DELETE", response = classOf[ContribGeocodeKey])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "c", dataType = "au.csiro.data61.gnaf.contrib.service.ContribGeocodeKey", paramType = "body", required = true)
  ))
  def deleteContribRoute(key: ContribGeocodeKey) = {
    val f = db.run(qGet(key.id, key.version).delete)
    complete {
      f.map[ToResponseMarshallable] { cnt =>
        if (cnt == 1) key
        else BadRequest -> s"key = $key not found"
      }
    }
  }

  @ApiOperation(value = "Update a contributed geocode for an addressSitePid", nickname = "update", httpMethod = "PUT", response = classOf[ContribGeocode])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "c", dataType = "au.csiro.data61.gnaf.contrib.service.ContribGeocode", paramType = "body", required = true)
  ))
  def updateContribRoute(c: ContribGeocode) = {
    val c2 = c.copy(version = c.version + 1, dateCreated = System.currentTimeMillis)
    val f = db.run(qGet(c.id.get, c.version).update(toAddressSiteGeocodeRow(c2)))
    complete {
      f.map[ToResponseMarshallable] { cnt =>
        if (cnt == 1) c2
        else s"id = ${c.id}, version = ${c.version} not found"
      }
    }
  }
  
  val routes = pathPrefix("contrib") {
    (post & entity(as[ContribGeocode])) { createContribRoute } ~
    (get & path(Segment)) { listRoute } ~
    (delete & entity(as[ContribGeocodeKey])) { deleteContribRoute } ~
    (put & entity(as[ContribGeocode])) { updateContribRoute }
  }
}

object ContribService { self =>
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  
  val logger = Logging(system, getClass)
  val config = ConfigFactory.load
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  object MyService extends ContribService(logger, config) {    
    val myRoutes = logRequestResult("GnafContrib") {
      cors(defaultSettings.copy(allowedMethods = HttpMethods.DELETE +: defaultSettings.allowedMethods)) {
        routes
      }
    }
  }
  
  // route: /api-docs/swagger.json
  object SwaggerService extends SwaggerHttpService with HasActorSystem {
    import scala.reflect.runtime.{ universe => ru }

    override implicit val actorSystem = self.system
    override implicit val materializer = self.materializer
    override val apiTypes = Seq(ru.typeOf[ContribService])
    override val host = interface + ":" + port
    override val info = Info(version = "1.0")

    val myRoutes = logRequestResult("Swagger") {  cors() { routes } }
  }

  def main(args: Array[String]): Unit = {
    MyService.createSchemaIfNotExists
    Http().bindAndHandle(MyService.myRoutes ~ SwaggerService.myRoutes, interface, port)
  }
}
