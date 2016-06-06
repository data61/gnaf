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

case class Geocode(geocodeTypeCode: Option[String], geocodeTypeDescription: Option[String], reliabilityCode: Int, isDefault: Boolean, latitude: Option[BigDecimal], longitude: Option[BigDecimal])

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
    
  val qGeocodes = {
    def q(addressDetailPid: Rep[String]) = for {
      ad <- AddressDetail if ad.addressDetailPid === addressDetailPid
      dg <- AddressDefaultGeocode if dg.addressDetailPid === addressDetailPid
      sg <- AddressSiteGeocode if sg.addressSitePid === ad.addressSitePid
    } yield (dg.geocodeTypeCode, sg.geocodeTypeCode, sg.reliabilityCode, sg.latitude, sg.longitude)
    Compiled(q _)
  }
  
  def geocodes(addressDetailPid: String)(implicit db: Database): Future[Seq[Geocode]] = {
    for {
      typ <- geocodeTypesFuture
      seq <- db.run(qGeocodes(addressDetailPid).result)
    } yield seq.map { x =>
      Geocode(x._2, x._2.map(typ), x._3, Some(x._1) == x._2, x._4, x._5)
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

/*
SELECT * FROM ADDRESS_DEFAULT_GEOCODE;
ADDRESS_DEFAULT_GEOCODE_PID  	DATE_CREATED  	DATE_RETIRED  	ADDRESS_DETAIL_PID  	GEOCODE_TYPE_CODE  	LONGITUDE  	LATITUDE  
3006445381	2012-11-05	null	GAACT714850059	FCS	149.02880402	-35.19539320
3006507266	2012-11-05	null	GAACT714850060	FCS	149.02854884	-35.19528929
3006444884	2012-11-05	null	GAACT714850061	FCS	149.02845788	-35.19516460
*/

object GnafService extends Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val logger = Logging(system, getClass)
  
  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
