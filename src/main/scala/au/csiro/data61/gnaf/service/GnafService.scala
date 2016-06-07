package au.csiro.data61.gnaf.service

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.IOException
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import spray.json.DefaultJsonProtocol
import au.csiro.data61.gnaf.db.GnafTables
import au.csiro.data61.gnaf.util.Util
import ch.megard.akka.http.cors.CorsDirectives.cors

case class Geocode(geocodeTypeCode: Option[String], geocodeTypeDescription: Option[String], reliabilityCode: Option[Int], isDefault: Boolean, latitude: Option[BigDecimal], longitude: Option[BigDecimal])

trait Protocols extends DefaultJsonProtocol {
  implicit val geocodeFormat = jsonFormat6(Geocode.apply)
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
      sg.map { x => Geocode(x.geocodeTypeCode, x.geocodeTypeCode.map(typ), Some(x.reliabilityCode), Some(dg.geocodeTypeCode) == x.geocodeTypeCode, x.latitude, x.longitude) }
        .getOrElse(Geocode(Some(dg.geocodeTypeCode), Some(typ(dg.geocodeTypeCode)), None, true, dg.latitude, dg.longitude)) // handle the no AddressSiteGeocode case
    }.sortBy(!_.isDefault)
  }
  
  val routes = { cors() {
    logRequestResult("GnafService") {
      pathPrefix("addressGeocode") {
        (get & path(Segment)) { addressDetailPid =>
          complete {
            geocodes(addressDetailPid)
          }
        }
      }
    }
  } }
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
