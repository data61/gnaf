package au.com.data61.gnaf.indexer

import scala.concurrent.{ Future, Await }
import scala.concurrent.ExecutionContext.Implicits.global
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

// Organize Imports deletes this, so make it easy to restore ...
// import slick.collection.heterogeneous.syntax.::

object Main {
  val log = Logging.getLogger(getClass)
  
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
      opt[String]('u', "dburl") action { (x, c) =>
        c.copy(dburl = x)
      } text (s"database URL, default ${Config().dburl}")
      help("help") text ("prints this usage text")
    }
    parser.parse(args, Config()) foreach run
  }

  case class PreNumSuf(prefix: Option[String], number: Option[Int], suffix: Option[String])
  case class Street(name: String, typeCode: Option[String], suffixCode: Option[String])
  case class Address(addressSiteName: Option[String], buildingName: Option[String],
      flatTypeCode: Option[String], flat: PreNumSuf, 
      levelName: Option[String], level: PreNumSuf,
      numberFirst: PreNumSuf,
      numberLast: PreNumSuf,
      streetLocalityPid: Option[String], street: Option[Street],
      localityPid: String, localityName: String, stateAbbreviation: String, stateName: String, postcode: Option[String])
  case class AddressVariants(address: Address, streetVariants: Seq[Street], localityVariants: Seq[String])
  
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
    
  val qLocality = {
    def q(localityPid: Rep[String]) = for (loc <- Locality if loc.localityPid === localityPid) yield loc
    Compiled(q _)
  }
  def locality(localityPid: String)(implicit db: Database): Future[LocalityRow] =
    db.run(qLocality(localityPid).result).map(_.head)
    
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
      
  def run(c: Config) = {
    for (database <- managed(Database.forConfig("gnafDb"))) {
      implicit val db = database
      
      val stream = db.stream(AddressDetail.filter(_.confidence > -1).result).mapResult {
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
            loc <- locality(localityPid)
            street <- street(streetLocalityPid)
            state <- state(loc.statePid)
            localityAliasName <- localityAliasName(localityPid)
            streetAlias <- streetAlias(streetLocalityPid)
          } yield AddressVariants(Address(
              addressSiteName, buildingName,
              flatTypeCode, PreNumSuf(flatNumberPrefix, flatNumber, flatNumberSuffix),
              levelName, PreNumSuf(levelNumberPrefix, levelNumber, levelNumberSuffix), 
              PreNumSuf(numberFirstPrefix, numberFirst, numberFirstSuffix),
              PreNumSuf(numberLastPrefix, numberLast, numberLastSuffix),
              streetLocalityPid, street,
              localityPid, loc.localityName, state.stateAbbreviation, state.stateName, postcode), streetAlias, localityAliasName)
        }
      }
      
      val done = stream.foreach(_.onComplete {
        case Success(a) => println(mapper.writeValueAsString(a))
        case Failure(e) => log.error("future failed", e)
      } )
      
      Await.result(done, 1.day)
    }
  }
}
