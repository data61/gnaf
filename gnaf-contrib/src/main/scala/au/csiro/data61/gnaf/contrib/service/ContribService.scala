package au.csiro.data61.gnaf.contrib.service

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.math.BigDecimal

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
import au.csiro.data61.gnaf.util.Util
import au.csiro.data61.gnaf.contrib.db.ContribTables
import ch.megard.akka.http.cors.CorsDirectives.cors
import ch.megard.akka.http.cors.CorsSettings.defaultSettings
import io.swagger.annotations.{ Api, ApiParam, ApiImplicitParams, ApiImplicitParam, ApiOperation }
import javax.ws.rs.{ Path, PathParam, DefaultValue }
import slick.dbio.DBIOAction
import slick.jdbc.ResultSetAction
import spray.json.DefaultJsonProtocol

case class ContribGeocode(id: Option[Long], contribStatus: String, addressSiteGeocodePid: Option[String], dateCreated: Long, version: Int, addressSitePid: String, geocodeTypeCode: String, longitude: BigDecimal, latitude: BigDecimal)
case class ContribGeocodeKey(id: Long, version: Int)

object JsonProtocol extends DefaultJsonProtocol {
  implicit val contribGeocodeFormat = jsonFormat9(ContribGeocode.apply)
  implicit val contribGeocodeKeyFormat = jsonFormat2(ContribGeocodeKey.apply)
}
import JsonProtocol._

@Api(value = "contrib", produces = "application/json")
@Path("contrib")
class ContribService(logger: LoggingAdapter, config: Config)(implicit system: ActorSystem, executor: ExecutionContextExecutor, materializer: Materializer) {
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

  @Path("{addressSitePid}")
  @ApiOperation(value = "List contributed geocodes for an addressSitePid", nickname = "list", 
      httpMethod = "GET", response = classOf[ContribGeocode], responseContainer = "List")
  def listRoute(
      @PathParam("addressSitePid")
      addressSitePid: String
  ) = {
    val f = db.run(qList(addressSitePid).result).map(_.map(toContribGeocode))
    complete { f }
  }

  val contribGeocodeWithId = (AddressSiteGeocode returning AddressSiteGeocode.map(_.id) )
  def toAddressSiteGeocodeRow(x: ContribGeocode) = AddressSiteGeocodeRow(x.id, x.contribStatus, x.addressSiteGeocodePid, new java.sql.Date(x.dateCreated), x.version, x.addressSitePid, x.geocodeTypeCode, x.longitude, x.latitude)
  
  @ApiOperation(value = "Add a new contributed geocode for an addressSitePid", nickname = "create",
      notes="""id, version and dateCreated input ignored & output set by system (however input values for version and dateCreated are still required). 
        
Example input (included here as @ApiParam(defaultValue) and @DefaultValue aren't working so far):
{
	"contribStatus":"Submitted",
	"addressSitePid":"712279621",
	"geocodeTypeCode":"EM",
	"longitude":149.1213974,
	"latitude":-35.280994199999995,
	"dateCreated":0,
	"version":0
}    
""",
      httpMethod = "POST", response = classOf[ContribGeocode])
  def createContribRoute(
    @ApiParam(value = "contribGeocode", required = true, defaultValue = "Fred")
    @DefaultValue("harry")
    c: ContribGeocode
  ) = {
    val c2 = (c.copy(dateCreated = System.currentTimeMillis, version = 1))
    val f = db.run(contribGeocodeWithId += toAddressSiteGeocodeRow(c2)).map(id => c2.copy(id = Some(id)))
    complete { f }
  }
  
  def qGet = {
    def q(id: Rep[Long], version: Rep[Int]) = AddressSiteGeocode.filter(x => x.id === id && x.version === version)
    Compiled(q _)
  }
  
  @ApiOperation(value = "Delete a contributed geocode for an addressSitePid", nickname = "delete", 
      notes="optimistic lock version must match to succeed", httpMethod = "DELETE", response = classOf[ContribGeocodeKey])
  def deleteContribRoute(
    @ApiParam(value = "contribGeocodeKey", required = true) 
    key: ContribGeocodeKey
  ) = {
    val f = db.run(qGet(key.id, key.version).delete)
    complete {
      f.map[ToResponseMarshallable] { cnt =>
        if (cnt == 1) key
        else BadRequest -> s"key = $key not found"
      }
    }
  }

  @ApiOperation(value = "Update a contributed geocode for an addressSitePid", nickname = "update",
      notes = """optimistic lock version must match to succeed.
        
dateCreated input ignored (but still required); version and dateCreated output set by system
""", 
      httpMethod = "PUT", response = classOf[ContribGeocode])
  def updateContribRoute(
    @ApiParam(value = "contribGeocode", required = true) 
    c: ContribGeocode
  ) = {
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

object ContribService {
  implicit val sys = ActorSystem()
  implicit val exec = sys.dispatcher
  implicit val mat = ActorMaterializer()
  
  val logger = Logging(sys, getClass)
  val config = ConfigFactory.load
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  val service = new ContribService(logger, config)
  
  // /api-docs/swagger.json
  val swagger = new SwaggerHttpService with HasActorSystem {
    import scala.reflect.runtime.{ universe => ru }

    override implicit val actorSystem = sys
    override implicit val materializer = mat
    override val apiTypes = Seq(ru.typeOf[ContribService])
    override val host = interface + ":" + port
    override val info = Info(version = "1.0")
  }

  def main(args: Array[String]): Unit = {
    service.createSchemaIfNotExists
    
    val routes = cors(defaultSettings.copy(allowedMethods = HttpMethods.DELETE +: defaultSettings.allowedMethods)) {
      logRequestResult("GnafContrib") { service.routes } ~
      logRequestResult("Swagger") { swagger.routes }
    }
    Http().bindAndHandle(routes, interface, port)
  }
}
