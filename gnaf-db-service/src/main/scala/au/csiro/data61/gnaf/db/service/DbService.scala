package au.csiro.data61.gnaf.db.service

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.math.BigDecimal

import com.github.swagger.akka.{ HasActorSystem, SwaggerHttpService }
import com.github.swagger.akka.model.Info
import com.typesafe.config.{ Config, ConfigFactory }

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.server.Directives.{ Segment, _enhanceRouteWithConcatenation, _segmentStringToPathMatcher, complete, get, logRequestResult, path, pathPrefix }
import akka.stream.{ ActorMaterializer, Materializer }
import au.csiro.data61.gnaf.db.GnafTables
import au.csiro.data61.gnaf.util.Util
import ch.megard.akka.http.cors.CorsDirectives.cors
import io.swagger.annotations.{ Api, ApiOperation }
import javax.ws.rs.{ Path, PathParam }
import spray.json.DefaultJsonProtocol

// for latitude: BigDecimal swagger type is number, but for Option[BigDecimal] swagger type is complex internal representation of scala.math.BigDecimal, so we avoid using Option here
case class Geocode(geocodeTypeCode: Option[String], geocodeTypeDescription: Option[String], reliabilityCode: Option[Int], isDefault: Boolean, latitude: BigDecimal, longitude: BigDecimal)

case class AddressType(addressSitePid: String, addressType: Option[String])
case class AddressTypeOpt(addressType: Option[AddressType])

case class GeocodeType(code: String, description: String)
case class GeocodeTypes(types: Seq[GeocodeType])

trait Protocols extends DefaultJsonProtocol {
  implicit val geocodeFormat = jsonFormat6(Geocode.apply)
  
  implicit val addressTypeFormat = jsonFormat2(AddressType.apply)
  implicit val addressTypeOptFormat = jsonFormat1(AddressTypeOpt.apply)
  
  implicit val geocodeTypeFormat = jsonFormat2(GeocodeType.apply)
  implicit val geocodeTypesFormat = jsonFormat1(GeocodeTypes.apply)
}

@Api(value = "gnaf", produces = "application/json")
@Path("gnaf")
class DbService(logger: LoggingAdapter, config: Config)(implicit system: ActorSystem, executor: ExecutionContextExecutor, materializer: Materializer) extends Protocols {

  object MyGnafTables extends {
    val profile = Util.getObject[slick.driver.JdbcProfile](config.getString("gnafDb.slickDriver")) // e.g. slick.driver.{H2Driver,PostgresDriver}
  } with GnafTables
  val gnafTables = MyGnafTables
  import gnafTables._
  import gnafTables.profile.api._
  
  implicit val db = Database.forConfig("gnafDb", config)
    
  // map code -> description
  lazy val geocodeTypesFuture: Future[Map[String, String]] = db.run(GeocodeTypeAut.result).map(_.map(t => t.code -> t.description.getOrElse(t.code)).toMap)
  
  @Path("geocodeType")
  @ApiOperation(value = "List geocode types", nickname = "geocodeType", httpMethod = "GET", response = classOf[GeocodeType], responseContainer = "List")
  def geocodeType = complete {
    geocodeTypesFuture.map { x =>
      GeocodeTypes(x.toSeq.map(GeocodeType.tupled))
    }
  }
  
  // left join because some addressDetailPid have no AddressSiteGeocode
  val qGeocodes = {
    def q(addressDetailPid: Rep[String]) = for {
      (ad, sg) <- AddressDetail joinLeft AddressSiteGeocode on (_.addressSitePid === _.addressSitePid) if ad.addressDetailPid === addressDetailPid
      dg <- AddressDefaultGeocode if dg.addressDetailPid === addressDetailPid
    } yield (dg, sg)
    Compiled(q _)
  }
  
  @Path("addressGeocode/{addressDetailPid}")
  @ApiOperation(value = "List geocodes for an addressSitePid", nickname = "addressGeocode", httpMethod = "GET", response = classOf[Geocode], responseContainer = "List")
  def addressGeocode(@PathParam("addressDetailPid") addressDetailPid: String) = {
    val f = for {
      typ <- geocodeTypesFuture
      seq <- db.run(qGeocodes(addressDetailPid).result)
    } yield seq.map { case (dg, sg) =>
      // should either have 1 (dg, None) or 1 or more (dg, Some(addressSiteGeocode)), the latitude & longitude values should not be None
      sg.map { x => Geocode(x.geocodeTypeCode, x.geocodeTypeCode.map(typ), Some(x.reliabilityCode), Some(dg.geocodeTypeCode) == x.geocodeTypeCode && dg.latitude == x.latitude && dg.longitude == x.longitude, x.latitude.getOrElse(0), x.longitude.getOrElse(0)) }
        .getOrElse(Geocode(Some(dg.geocodeTypeCode), Some(typ(dg.geocodeTypeCode)), None, true, dg.latitude.getOrElse(0), dg.longitude.getOrElse(0))) // handle the (dg, None) no AddressSiteGeocode case
    }.sortBy(!_.isDefault)
    
    complete { f }
  }
    
  lazy val addressTypesFuture = db.run(AddressTypeAut.result).map(_.map(t => t.code -> t.description.getOrElse(t.code)).toMap)
    
  val qAddressSite = {
    def q(addressDetailPid: Rep[String]) = for {
      ad <- AddressDetail if ad.addressDetailPid === addressDetailPid
      as <- AddressSite if as.addressSitePid === ad.addressSitePid
    } yield as
    Compiled(q _)
  }
  
  @Path("addressType/{addressDetailPid}")
  @ApiOperation(value = "AddressType for an addressSitePid", nickname = "addressType", httpMethod = "GET", response = classOf[AddressTypeOpt])
  def addressType(@PathParam("addressDetailPid") addressDetailPid: String) = {
    val f = for {
      typ <- addressTypesFuture
      asOpt <- db.run(qAddressSite(addressDetailPid).result.headOption)
    } yield AddressTypeOpt(asOpt.map(as => AddressType(as.addressSitePid, as.addressType.map(typ))))
    
    complete { f }
  }

  val routes = pathPrefix("gnaf") {
      pathPrefix("geocodeType") {
        get { geocodeType }
      } ~
      pathPrefix("addressGeocode") {
        (get & path(Segment)) { addressGeocode }
      } ~
      pathPrefix("addressType") {
        (get & path(Segment)) { addressType }
      }
  }
}

object DbService {
  implicit val sys = ActorSystem()
  implicit val exec = sys.dispatcher
  implicit val mat = ActorMaterializer()
  
  val logger = Logging(sys, getClass)
  val config = ConfigFactory.load
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")
  
  val service = new DbService(logger, config) {    
    val myRoutes =  { routes }
  }
  
  // /api-docs/swagger.json
  val swagger = new SwaggerHttpService with HasActorSystem {
    import scala.reflect.runtime.{ universe => ru }

    override implicit val actorSystem = sys
    override implicit val materializer = mat
    override val apiTypes = Seq(ru.typeOf[DbService])
    override val host = interface + ":" + port
    override val info = Info(version = "1.0")
  }

  def main(args: Array[String]): Unit = {
    val routes = cors() {
      logRequestResult("DbService") { service.routes } ~ 
      logRequestResult("Swagger") { swagger.routes }
    }
    Http().bindAndHandle(routes, interface, port)
  }
}
