package au.com.data61.gnaf.indexer

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }
import au.com.data61.gnaf.db.Tables._
import au.com.data61.gnaf.util.Logging
import resource.managed
import slick.collection.heterogeneous.HNil
import slick.collection.heterogeneous.syntax.::
import slick.driver.H2Driver.api._
import slick.driver.H2Driver.backend
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import slick.backend.DatabasePublisher
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import scala.math.BigDecimal

// Organize Imports deletes this, so make it easy to restore ...
// import slick.collection.heterogeneous.syntax.::

object Main {
  val log = Logging.getLogger(getClass)
  
  object MyExecutionContext {
    private val noOfThread = Runtime.getRuntime.availableProcessors * 3
    implicit val ioThreadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(noOfThread), e => log.error("ThreadPool", e))
  }
  import MyExecutionContext.ioThreadPool
  
  // using Jackson for simpler JSON than scala-pickle (which adds type info required for unpickling)
  val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m
  }
  
  /** result of command line option processing */
  case class Config(dburl: String = "jdbc:h2:file:~/sw/gnaf/data/gnaf")
  
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("gnaf-indexer") {
      head("gnaf-indexer", "0.x")
      note("Creates Lucene index from gnaf database.")
      opt[String]('u', "dburl") action { (x, c) =>  // not used, using application.conf
        c.copy(dburl = x)
      } text (s"database URL, default ${Config().dburl}")
      help("help") text ("prints this usage text")
    }
    parser.parse(args, Config()) foreach run   
    
    ioThreadPool.shutdown
    ioThreadPool.awaitTermination(5, TimeUnit.SECONDS)
    log.info("thread pool terminated")
  }

  case class PreNumSuf(prefix: Option[String], number: Option[Int], suffix: Option[String])
  case class Street(name: String, typeCode: Option[String], typeName: Option[String], suffixCode: Option[String], suffixName: Option[String])
  case class LocalityVariant(localityName: String)
  case class Location(lat: BigDecimal, lon: BigDecimal)
  case class Address(addressDetailPid: String, addressSiteName: Option[String], buildingName: Option[String],
      flatTypeCode: Option[String], flatTypeName: Option[String], flat: PreNumSuf, 
      levelTypeCode: Option[String], levelTypeName: Option[String], level: PreNumSuf,
      numberFirst: PreNumSuf, numberLast: PreNumSuf,
      street: Option[Street], localityName: String, stateAbbreviation: String, stateName: String, postcode: Option[String],
      aliasPrincipal: Option[Char], primarySecondary: Option[Char],
      location: Option[Location], streetVariant: Seq[Street], localityVariant: Seq[LocalityVariant])
  
  val qAddressDetail = {
    def q(localityPid: Rep[String]) = for {
      ((((ad, lta), as), sl), adg) <- AddressDetail joinLeft 
        LevelTypeAut on (_.levelTypeCode === _.code) joinLeft
        AddressSite on (_._1.addressSitePid === _.addressSitePid) joinLeft
        StreetLocality on (_._1._1.streetLocalityPid === _.streetLocalityPid) joinLeft
        AddressDefaultGeocode on (_._1._1._1.addressDetailPid === _.addressDetailPid)
      if ad.localityPid === localityPid
    } yield (
      ad, 
      lta.map(_.name), 
      as.map(_.addressSiteName), 
      sl.map(sl => (sl.streetName, sl.streetTypeCode, sl.streetSuffixCode)), 
      adg.map(adg => (adg.latitude, adg.longitude))
    )
    Compiled(q _)
  }
      
  // 1 to 0..1 handled above in qAddressDetail, 1 to 0..n handled below 
  
  val qLocalityAliasName = {
    def q(localityPid: Rep[String]) = for (la <- LocalityAlias if la.localityPid === localityPid) yield la.name
    Compiled(q _)
  }
  def localityVariant(localityPid: String)(implicit db: Database): Future[Seq[LocalityVariant]] =
    db.run(qLocalityAliasName(localityPid).result).map(_.map(name => LocalityVariant(name)))
    
  val qStreetLocalityAlias = {
    def q(streetLocalityPid: Rep[String]) = for (sla <- StreetLocalityAlias if sla.streetLocalityPid === streetLocalityPid) yield (sla.streetName, sla.streetTypeCode, sla.streetSuffixCode)
    Compiled(q _)
  }
  def streetLocalityAlias(streetLocalityPid: Option[String])(implicit db: Database): Future[Seq[(String, Option[String], Option[String])]] = {
    streetLocalityPid.map { pid => 
      db.run(qStreetLocalityAlias(pid).result) 
    }.getOrElse(Future(Seq.empty))
  }
        
  def run(c: Config) = {
    for (database <- managed(Database.forConfig("gnafDb"))) {
      implicit val db = database
      
      val stateMap = db.run((for (s <- State) yield s.statePid -> (s.stateAbbreviation, s.stateName)).result).map(_.toMap)
      val flatTypeMap = db.run((for (f <- FlatTypeAut) yield f.code -> f.name).result).map(_.toMap)
      val streetTypeMap = db.run((for (s <- StreetTypeAut) yield s.code -> s.name).result).map(_.toMap)
      val streetSuffixMap = db.run((for (s <- StreetSuffixAut) yield s.code -> s.name).result).map(_.toMap)
      
      
      // when I try to stream all AddressDetail rows, I don't get any rows in a reasonable time (seems to hang but CPU is busy).
      /*
        http://stackoverflow.com/questions/24787119/how-to-set-h2-to-stream-resultset
        H2 currently does not support server side cursors. However, it buffers large result sets to disk (as a separate file, or as a temporary table). The disadvantage is speed, but it should not be a memory usage problems.
        
        You can set the size of the when H2 will buffer to disk using set max_memory_rows. You can append that to the database URL: jdbc:h2:~/test;max_memory_rows=100000.
        
        A workaround is usually to use "keyset paging" as described in the presentation "Pagination Done the Right Way". That would mean running multiple queries instead of one.
        
        http://www.h2database.com/html/advanced.html
        Before the result is returned to the application, all rows are read by the database. Server side cursors are not supported currently.
        
        http://www.h2database.com/javadoc/org/h2/engine/SysProperties.html?highlight=max_memory_rows&search=max_memory_rows#h2.maxMemoryRows
        System property h2.maxMemoryRows (default: 40000 per GB of available RAM).
        
        So if we set -Xmx3G  and partition by LOCALITY_PID we should be OK:
        SELECT LOCALITY_PID , count(*) cnt FROM ADDRESS_DETAIL group by LOCALITY_PID order by cnt desc limit 10;
        LOCALITY_PID  	CNT  
        VIC1634	95004
        NSW3749	44656
        QLD2772	34712
        VIC2319	32516
        NSW3272	31914
        VIC2158	26941
        SA2	26500
        VIC2318	25715
        QLD2676	25302
        NSW1310	23249
        
        There are 16398 LOCALITY rows.
       */
      val locDone = db.stream((for (loc <- Locality) yield (loc.localityPid, loc.localityName, loc.statePid)).result).foreach { case (localityPid, localityName, statePid) =>
        val state = stateMap.map(_.apply(statePid))
        val locVariant = localityVariant(localityPid)
        
        val adStream = db.stream(qAddressDetail(localityPid).result).mapResult { case (
          // copied from AddressDetail.*
          addressDetailPid :: dateCreated :: dateLastModified :: dateRetired :: buildingName :: lotNumberPrefix :: lotNumber :: lotNumberSuffix :: 
          flatTypeCode :: flatNumberPrefix :: flatNumber :: flatNumberSuffix :: 
          levelTypeCode :: levelNumberPrefix :: levelNumber :: levelNumberSuffix :: 
          numberFirstPrefix :: numberFirst :: numberFirstSuffix :: 
          numberLastPrefix :: numberLast :: numberLastSuffix :: 
          streetLocalityPid :: locationDescription :: localityPid :: aliasPrincipal :: postcode :: privateStreet :: legalParcelId :: confidence :: 
          addressSitePid :: levelGeocodedCode :: propertyPid :: gnafPropertyPid :: primarySecondary :: HNil,
          levelTypeName,
          addressSiteName,
          street,
          location
          ) =>
          for {
            (stateAbbreviation, stateName) <- state
            ftm <- flatTypeMap
            stm <- streetTypeMap
            ssm <- streetSuffixMap
            locVar <- locVariant
            sla <- streetLocalityAlias(streetLocalityPid)
         } yield Address(
           addressDetailPid, addressSiteName.flatten, buildingName,
           flatTypeCode, flatTypeCode.map(ftm), PreNumSuf(flatNumberPrefix, flatNumber, flatNumberSuffix),
           levelTypeCode, levelTypeName, PreNumSuf(levelNumberPrefix, levelNumber, levelNumberSuffix), 
           PreNumSuf(numberFirstPrefix, numberFirst, numberFirstSuffix),
           PreNumSuf(numberLastPrefix, numberLast, numberLastSuffix),
           street.map(s => Street(s._1, s._2, s._2.map(stm), s._3, s._3.map(ssm))),
           localityName, stateAbbreviation, stateName, postcode,
           aliasPrincipal, primarySecondary,
           location.flatMap {
             case (Some(lat), Some(lon)) => Some(Location(lat, lon))
             case _ => None
           },
           sla.map(s => Street(s._1, s._2, s._2.map(stm), s._3, s._3.map(ssm))),
           locVar)
        }
        
        val adDone = adStream.foreach(_.onComplete {
          case Success(a) => println(mapper.writeValueAsString(a))
          case Failure(e) => log.error("future failed", e)
        } )
        // To my surprise the above println appears to be thread-safe, I was expecting to have to do something more
        Await.result(adDone, 15.minute)
        log.info(s"locality $localityName done")
      }
      Await.result(locDone, 2.hour)
      log.info("all done")
      Thread.sleep(1000) // getting 3-5 address lines printed after "futures complete", so Await.result isn't quite working
    }
  }
}
