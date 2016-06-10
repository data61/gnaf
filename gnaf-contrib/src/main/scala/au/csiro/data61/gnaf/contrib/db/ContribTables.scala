package au.csiro.data61.gnaf.contrib.db
// AUTO-GENERATED Slick data model
/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait ContribTables {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = ContribAddressSiteGeocode.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table ContribAddressSiteGeocode
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param contribStatus Database column CONTRIB_STATUS SqlType(VARCHAR), Length(15,true)
   *  @param addressSiteGeocodePid Database column ADDRESS_SITE_GEOCODE_PID SqlType(VARCHAR), Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param addressSitePid Database column ADDRESS_SITE_PID SqlType(VARCHAR), Length(15,true)
   *  @param geocodeTypeCode Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true)
   *  @param longitude Database column LONGITUDE SqlType(DECIMAL)
   *  @param latitude Database column LATITUDE SqlType(DECIMAL) */
  case class ContribAddressSiteGeocodeRow(id: Long, contribStatus: String, addressSiteGeocodePid: Option[String], dateCreated: java.sql.Date, addressSitePid: String, geocodeTypeCode: String, longitude: scala.math.BigDecimal, latitude: scala.math.BigDecimal)
  /** GetResult implicit for fetching ContribAddressSiteGeocodeRow objects using plain SQL queries */
  implicit def GetResultContribAddressSiteGeocodeRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[java.sql.Date], e4: GR[scala.math.BigDecimal]): GR[ContribAddressSiteGeocodeRow] = GR{
    prs => import prs._
    ContribAddressSiteGeocodeRow.tupled((<<[Long], <<[String], <<?[String], <<[java.sql.Date], <<[String], <<[String], <<[scala.math.BigDecimal], <<[scala.math.BigDecimal]))
  }
  /** Table description of table CONTRIB_ADDRESS_SITE_GEOCODE. Objects of this class serve as prototypes for rows in queries. */
  class ContribAddressSiteGeocode(_tableTag: Tag) extends Table[ContribAddressSiteGeocodeRow](_tableTag, "CONTRIB_ADDRESS_SITE_GEOCODE") {
    def * = (id, contribStatus, addressSiteGeocodePid, dateCreated, addressSitePid, geocodeTypeCode, longitude, latitude) <> (ContribAddressSiteGeocodeRow.tupled, ContribAddressSiteGeocodeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(contribStatus), addressSiteGeocodePid, Rep.Some(dateCreated), Rep.Some(addressSitePid), Rep.Some(geocodeTypeCode), Rep.Some(longitude), Rep.Some(latitude)).shaped.<>({r=>import r._; _1.map(_=> ContribAddressSiteGeocodeRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column CONTRIB_STATUS SqlType(VARCHAR), Length(15,true) */
    val contribStatus: Rep[String] = column[String]("CONTRIB_STATUS", O.Length(15,varying=true))
    /** Database column ADDRESS_SITE_GEOCODE_PID SqlType(VARCHAR), Length(15,true) */
    val addressSiteGeocodePid: Rep[Option[String]] = column[Option[String]]("ADDRESS_SITE_GEOCODE_PID", O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column ADDRESS_SITE_PID SqlType(VARCHAR), Length(15,true) */
    val addressSitePid: Rep[String] = column[String]("ADDRESS_SITE_PID", O.Length(15,varying=true))
    /** Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true) */
    val geocodeTypeCode: Rep[String] = column[String]("GEOCODE_TYPE_CODE", O.Length(4,varying=true))
    /** Database column LONGITUDE SqlType(DECIMAL) */
    val longitude: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("LONGITUDE")
    /** Database column LATITUDE SqlType(DECIMAL) */
    val latitude: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("LATITUDE")
  }
  /** Collection-like TableQuery object for table ContribAddressSiteGeocode */
  lazy val ContribAddressSiteGeocode = new TableQuery(tag => new ContribAddressSiteGeocode(tag))
}
