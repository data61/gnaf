package au.csiro.data61.gnaf.service

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.math.BigDecimal

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonMarshaller
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.server.Directives.{ Segment, complete, enhanceRouteWithConcatenation, get, logRequestResult, path, pathPrefix, segmentStringToPathMatcher }
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.stream.{ ActorMaterializer, Materializer }
import au.csiro.data61.gnaf.common.db.GnafTables
import au.csiro.data61.gnaf.common.util.Util
import ch.megard.akka.http.cors.CorsDirectives.cors
import spray.json.DefaultJsonProtocol

case class Geocode(geocodeTypeCode: Option[String], geocodeTypeDescription: Option[String], reliabilityCode: Option[Int], isDefault: Boolean, latitude: Option[BigDecimal], longitude: Option[BigDecimal])

case class AddressType(addressSitePid: String, addressType: Option[String])
case class AddressTypeOpt(addressType: Option[AddressType])

// TODO: next two also needed in gnaf-contrib so maybe move them all to gnaf-common?
case class GeocodeType(code: String, description: String)
case class GeocodeTypes(types: Seq[GeocodeType])

trait Protocols extends DefaultJsonProtocol {
  implicit val geocodeFormat = jsonFormat6(Geocode.apply)
  
  implicit val addressTypeFormat = jsonFormat2(AddressType.apply)
  implicit val addressTypeOptFormat = jsonFormat1(AddressTypeOpt.apply)
  
  implicit val geocodeTypeFormat = jsonFormat2(GeocodeType.apply)
  implicit val geocodeTypesFormat = jsonFormat1(GeocodeTypes.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  val config = ConfigFactory.load
  val logger: LoggingAdapter
  
  object MyGnafTables extends {
    val profile = Util.getObject[slick.driver.JdbcProfile](config.getString("gnafDb.slickDriver")) // e.g. slick.driver.{H2Driver,PostgresDriver}
  } with GnafTables
  val gnafTables = MyGnafTables
  import gnafTables._
  import gnafTables.profile.api._
  
  implicit val db = Database.forConfig("gnafDb", config)
    
  // map code -> description
  def geocodeTypes()(implicit db: Database): Future[Map[String, String]] = {
    db.run(GeocodeTypeAut.result).map(_.map(t => t.code -> t.description.getOrElse(t.code)).toMap)
  }
  lazy val geocodeTypesFuture = geocodeTypes
  
  // left join because some addressDetailPid have no AddressSiteGeocode
  val qGeocodes = {
    def q(addressDetailPid: Rep[String]) = for {
      (ad, sg) <- AddressDetail joinLeft AddressSiteGeocode on (_.addressSitePid === _.addressSitePid) if ad.addressDetailPid === addressDetailPid
      dg <- AddressDefaultGeocode if dg.addressDetailPid === addressDetailPid
    } yield (dg, sg)
    Compiled(q _)
  }
  
  def geocodes(addressDetailPid: String)(implicit db: Database): Future[Seq[Geocode]] = {
    for {
      typ <- geocodeTypesFuture
      seq <- db.run(qGeocodes(addressDetailPid).result)
    } yield seq.map { case (dg, sg) =>
      sg.map { x => Geocode(x.geocodeTypeCode, x.geocodeTypeCode.map(typ), Some(x.reliabilityCode), Some(dg.geocodeTypeCode) == x.geocodeTypeCode && dg.latitude == x.latitude && dg.longitude == x.longitude, x.latitude, x.longitude) }
        .getOrElse(Geocode(Some(dg.geocodeTypeCode), Some(typ(dg.geocodeTypeCode)), None, true, dg.latitude, dg.longitude)) // handle the no AddressSiteGeocode case
    }.sortBy(!_.isDefault)
  }
  
  def addressTypes()(implicit db: Database): Future[Map[String, String]] = {
    db.run(AddressTypeAut.result).map(_.map(t => t.code -> t.description.getOrElse(t.code)).toMap)
  }
  lazy val addressTypesFuture = addressTypes
    
  val qAddressSite = {
    def q(addressDetailPid: Rep[String]) = for {
      ad <- AddressDetail if ad.addressDetailPid === addressDetailPid
      as <- AddressSite if as.addressSitePid === ad.addressSitePid
    } yield as
    Compiled(q _)
  }
  
  def addressType(addressDetailPid: String)(implicit db: Database): Future[AddressTypeOpt] = {
    for {
      typ <- addressTypesFuture
      asOpt <- db.run(qAddressSite(addressDetailPid).result.headOption)
    } yield AddressTypeOpt(asOpt.map(as => AddressType(as.addressSitePid, as.addressType.map(typ))))
  }

  // val corsSettings = CorsSettings.defaultSettings.copy(allowGenericHttpRequests = true, allowedMethods = List(HttpMethods.GET, HttpMethods.OPTIONS), allowedOrigins = HttpOriginRange.*)
  val routes = {
    logRequestResult("GnafService") {  cors() {
      pathPrefix("addressGeocode") {
        (get & path(Segment)) { addressDetailPid =>
          complete {
            geocodes(addressDetailPid)
          }
        }
      } ~
      pathPrefix("addressType") {
        (get & path(Segment)) { addressDetailPid =>
          complete {
            addressType(addressDetailPid)
          }
        }
      } ~
      pathPrefix("geocodeTypes") {
        get {
          complete {
            geocodeTypes.map { x =>
              GeocodeTypes(x.toSeq.map(GeocodeType.tupled))
            }
          }
        }
      }
    } }
  }
}

object GnafService extends Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val logger = Logging(system, getClass)
  
  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
