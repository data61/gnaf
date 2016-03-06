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
  case class Street(name: String, typeCode: Option[String], suffixCode: Option[String])
  case class Address(addressDetailPid: String, addressSiteName: Option[String], buildingName: Option[String],
      flatTypeCode: Option[String], flat: PreNumSuf, 
      levelName: Option[String], level: PreNumSuf,
      numberFirst: PreNumSuf, numberLast: PreNumSuf,
      street: Option[Street], localityName: String, stateAbbreviation: String, stateName: String, postcode: Option[String],
      latitude: Option[scala.math.BigDecimal], longitude: Option[scala.math.BigDecimal], streetVariants: Seq[Street], localityVariants: Seq[String])
  
  val qAddressDetail = {
    def q(localityPid: Rep[String]) = for (ad <- AddressDetail if ad.localityPid === localityPid) yield ad
    Compiled(q _)
  }
  
  val qLevelName = {
    def q(code: Rep[String]) = for (lta <- LevelTypeAut if lta.code === code) yield lta.name
    Compiled(q _)
  }
  def levelName(code: Option[String])(implicit db: Database): Future[Option[String]] =
    code.map(c => db.run(qLevelName(c).result).map(_.headOption)).getOrElse(Future(None))
  
  val qAddressSiteName = {
    def q(addressSitePid: Rep[String]) = for (as <- AddressSite if as.addressSitePid === addressSitePid) yield as.addressSiteName
    Compiled(q _)
  }
  def addressSiteName(addressSitePid: String)(implicit db: Database): Future[Option[String]] =
    db.run(qAddressSiteName(addressSitePid).result).map(_.headOption.flatten)
    
  val qStreetLocality = {
    def q(streetLocalityPid: Rep[String]) = StreetLocality.filter(_.streetLocalityPid === streetLocalityPid)
    Compiled(q _)
  }
  def street(streetLocalityPid: Option[String])(implicit db: Database): Future[Option[Street]] = {
    streetLocalityPid.map { pid => 
      db.run(qStreetLocality(pid).result).map(seq => {
        val sl = seq.head
        Some(Street(sl.streetName, sl.streetTypeCode, sl.streetSuffixCode))
      })
    }.getOrElse(Future(None))
  }
    
  val qState = {
    def q(statePid: Rep[String]) = State.filter(_.statePid === statePid)
    Compiled(q _)
  }
  def state(statePid: String)(implicit db: Database): Future[StateRow] = 
    db.run(qState(statePid).result).map(_.head)   
    
  val qLocalityAliasName = {
    def q(localityPid: Rep[String]) = for (la <- LocalityAlias if la.localityPid === localityPid) yield la.name
    Compiled(q _)
  }
  def localityAliasName(localityPid: String)(implicit db: Database): Future[Seq[String]] =
    db.run(qLocalityAliasName(localityPid).result)
    
  val qStreetLocalityAlias = {
    def q(streetLocalityPid: Rep[String]) = StreetLocalityAlias.filter(_.streetLocalityPid === streetLocalityPid)
    Compiled(q _)
  }
  def streetAlias(streetLocalityPid: Option[String])(implicit db: Database): Future[Seq[Street]] = {
    streetLocalityPid.map { pid => 
      db.run(qStreetLocalityAlias(pid).result).map(_.map(sla => Street(sla.streetName, sla.streetTypeCode, sla.streetSuffixCode)))     
    }.getOrElse(Future(Seq.empty))
  }
  
  val qAddressDefaultGeocode = {
    def q(addressDetailPid: Rep[String]) = for (adg <- AddressDefaultGeocode if adg.addressDetailPid === addressDetailPid) yield adg
    Compiled(q _)
  }
  def addressDefaultGeocode(addressDetailPid: String)(implicit db: Database): Future[Option[AddressDefaultGeocodeRow]] =
    db.run(qAddressDefaultGeocode(addressDetailPid).result).map(_.headOption)
      
  def run(c: Config) = {
    for (database <- managed(Database.forConfig("gnafDb"))) {
      implicit val db = database
      
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
        val adStream = db.stream(qAddressDetail(localityPid).result).mapResult {
          // copied from AddressDetail.*
          case addressDetailPid :: dateCreated :: dateLastModified :: dateRetired :: buildingName :: lotNumberPrefix :: lotNumber :: lotNumberSuffix :: 
            flatTypeCode :: flatNumberPrefix :: flatNumber :: flatNumberSuffix :: 
            levelTypeCode :: levelNumberPrefix :: levelNumber :: levelNumberSuffix :: 
            numberFirstPrefix :: numberFirst :: numberFirstSuffix :: 
            numberLastPrefix :: numberLast :: numberLastSuffix :: 
            streetLocalityPid :: locationDescription :: localityPid :: aliasPrincipal :: postcode :: privateStreet :: legalParcelId :: confidence :: 
            addressSitePid :: levelGeocodedCode :: propertyPid :: gnafPropertyPid :: primarySecondary :: HNil => {
              
            for {
              levelName <- levelName(levelTypeCode)
              addressSiteName <- addressSiteName(addressSitePid)
              street <- street(streetLocalityPid)
              state <- state(statePid)
              localityAliasName <- localityAliasName(localityPid)
              streetAlias <- streetAlias(streetLocalityPid)
              adg <- addressDefaultGeocode(addressDetailPid)
            } yield Address(
                addressDetailPid, addressSiteName, buildingName,
                flatTypeCode, PreNumSuf(flatNumberPrefix, flatNumber, flatNumberSuffix),
                levelName, PreNumSuf(levelNumberPrefix, levelNumber, levelNumberSuffix), 
                PreNumSuf(numberFirstPrefix, numberFirst, numberFirstSuffix),
                PreNumSuf(numberLastPrefix, numberLast, numberLastSuffix),
                street, localityName, state.stateAbbreviation, state.stateName, postcode, adg.flatMap(_.latitude), adg.flatMap(_.longitude), streetAlias, localityAliasName)
          }
            
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
