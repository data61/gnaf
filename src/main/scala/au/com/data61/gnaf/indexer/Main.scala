package au.com.data61.gnaf.indexer

import java.util.concurrent.{ ConcurrentLinkedQueue, Executors, TimeUnit }

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.DurationInt
import scala.math.BigDecimal
import scala.util.{ Failure, Success }

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import au.com.data61.gnaf.db.Tables._
import au.com.data61.gnaf.util.Logging
import resource.managed
import slick.collection.heterogeneous.HNil
import slick.collection.heterogeneous.syntax.::
import slick.driver.H2Driver.api._
import slick.driver.H2Driver.backend


// Organize Imports deletes this, so make it easy to restore ...
// import slick.collection.heterogeneous.syntax.::

object Main {
  val log = Logging.getLogger(getClass)

  // use a bounded blocking queue
  // http://blog.quantifind.com/instantiations-of-scala-futures
  // I unsuccessfully tried a small queue to limit concurrent locations and a larger queue for everything else
  
//  def pool(queueCapacity: Int) = {
//    val numWorkers = sys.runtime.availableProcessors
//    val p = new ThreadPoolExecutor(numWorkers, numWorkers, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue[Runnable](queueCapacity) {
//      override def offer(e: Runnable) = {
//        put(e) // may block until queue has space
//        true
//      }
//    } )
//    (p, ExecutionContext.fromExecutorService(p))
//  }
  
  // warning against even trying!
  // https://github.com/alexandru/scala-best-practices/blob/master/sections/4-concurrency-parallelism.md
  
  // with an unbounded queue
  implicit val pool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4), e => log.error("ThreadPool", e))

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
      opt[String]('u', "dburl") action { (x, c) => // not used, using application.conf
        c.copy(dburl = x)
      } text (s"database URL, default ${Config().dburl}")
      help("help") text ("prints this usage text")
    }
    parser.parse(args, Config()) foreach run

    pool.shutdown
    pool.awaitTermination(5, TimeUnit.SECONDS)
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
      adg.map(adg => (adg.latitude, adg.longitude)))
    Compiled(q _)
  }
  
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
    for (db <- managed(Database.forConfig("gnafDb"))) {
      doAll()(db)
    }
  }

  /*
  When I try to stream all AddressDetail rows, I don't get any rows in a reasonable time (seems to hang but CPU is busy).
  
  http://stackoverflow.com/questions/24787119/how-to-set-h2-to-stream-resultset
  H2 currently does not support server side cursors. However, it buffers large result sets to disk (as a separate file, or as a temporary table). The disadvantage is speed, but it should not be a memory usage problems.
  
  You can set the size of the when H2 will buffer to disk using set max_memory_rows. You can append that to the database URL: jdbc:h2:~/test;max_memory_rows=100000.
  
  A workaround is usually to use "keyset paging" as described in the presentation "Pagination Done the Right Way". That would mean running multiple queries instead of one.
  
  http://www.h2database.com/html/advanced.html
  Before the result is returned to the application, all rows are read by the database. Server side cursors are not supported currently.
  
  http://www.h2database.com/javadoc/org/h2/engine/SysProperties.html?highlight=max_memory_rows&search=max_memory_rows#h2.maxMemoryRows
  System property h2.maxMemoryRows (default: 40000 per GB of available RAM).
  
  So if we set -Xmx3G  and partition by LOCALITY_PID we should be OK:
  There are 16398 LOCALITY rows and max ADDRESS_DETAILs for a LOCALITY is 95004.
  execut
  SELECT LOCALITY_PID , count(*) cnt FROM ADDRESS_DETAIL group by LOCALITY_PID order by cnt desc limit 3;
  LOCALITY_PID    CNT  
  VIC1634 95004
  NSW3749 44656
  QLD2772 34712
 */

  type FutStrMap = Future[Map[String, String]]

  def doAll()(implicit db: Database) = {
    // These code -> name mappings are all small enough to keep in memory
    val stateMap: Future[Map[String, (String, String)]] = db.run((for (s <- State) yield s.statePid -> (s.stateAbbreviation, s.stateName)).result).map(_.toMap)
    val flatTypeMap: FutStrMap = db.run((for (f <- FlatTypeAut) yield f.code -> f.name).result).map(_.toMap)
    val streetTypeMap: FutStrMap = db.run((for (s <- StreetTypeAut) yield s.code -> s.name).result).map(_.toMap)
    val streetSuffixMap: FutStrMap = db.run((for (s <- StreetSuffixAut) yield s.code -> s.name).result).map(_.toMap)

    val futures = new ConcurrentLinkedQueue[Future[Unit]]
    val localitiesDone = db.stream((for (loc <- Locality) yield (loc.localityPid, loc.localityName, loc.statePid)).result).foreach {
      case (localityPid, localityName, statePid) =>
        log info s"Running locality $localityName ..."
        val locDone = doLocation(localityPid, localityName, statePid, stateMap, flatTypeMap, streetTypeMap, streetSuffixMap)
        locDone.onComplete {
          case Success(a) => log info s"locality $localityName done."
          case Failure(e) => log.error(s"future for locality $localityName failed", e)
        }
        futures add locDone
        Await.result(locDone, 2.minute) // shouldn't be needed, but localities stream is filling up the queue so 
    }
    localitiesDone.onComplete {
      case Success(a) => log info "all localities stream complete"
      case Failure(e) => log.error(s"future for all localities failed", e)
    }
    futures add localitiesDone
    Await.result(Future.sequence(collectionAsScalaIterable(futures)), 2.hour)
    log info "all done"
    Thread sleep 1000 // getting 3-5 address lines printed after "all done", so Await.result isn't quite working
  }

  def doLocation(localityPid: String, localityName: String, statePid: String,
                 stateMap: Future[Map[String, (String, String)]],
                 flatTypeMap: FutStrMap,
                 streetTypeMap: FutStrMap,
                 streetSuffixMap: FutStrMap)(implicit db: Database) = {
    val state = stateMap.map(_.apply(statePid))
    val locVariant = localityVariant(localityPid)
    
    db.stream(qAddressDetail(localityPid).result).foreach {

      case (
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

        val addr: Future[Address] = for {
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
            case _                      => None
          },
          sla.map(s => Street(s._1, s._2, s._2.map(stm), s._3, s._3.map(ssm))),
          locVar)

        addr.onComplete {
          case Success(a) => println(mapper.writeValueAsString(a)) // println appears to be synchronized
          case Failure(e) => log.error("future failed", e)
        }
    }
  }

}
