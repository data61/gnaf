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
  lazy val schema: profile.SchemaDescription = AddressSiteGeocode.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table AddressSiteGeocode
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param contribStatus Database column CONTRIB_STATUS SqlType(VARCHAR), Length(15,true)
   *  @param addressSiteGeocodePid Database column ADDRESS_SITE_GEOCODE_PID SqlType(VARCHAR), Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param version Database column VERSION SqlType(INTEGER)
   *  @param addressSitePid Database column ADDRESS_SITE_PID SqlType(VARCHAR), Length(15,true)
   *  @param geocodeTypeCode Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true)
   *  @param longitude Database column LONGITUDE SqlType(DECIMAL)
   *  @param latitude Database column LATITUDE SqlType(DECIMAL) */
  case class AddressSiteGeocodeRow(id: Option[Long], contribStatus: String, addressSiteGeocodePid: Option[String], dateCreated: java.sql.Date, version: Int, addressSitePid: String, geocodeTypeCode: String, longitude: scala.math.BigDecimal, latitude: scala.math.BigDecimal)
  /** GetResult implicit for fetching AddressSiteGeocodeRow objects using plain SQL queries */
  implicit def GetResultAddressSiteGeocodeRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[java.sql.Date], e4: GR[Int], e5: GR[scala.math.BigDecimal]): GR[AddressSiteGeocodeRow] = GR{
    prs => import prs._
    AddressSiteGeocodeRow.tupled((<<[Option[Long]], <<[String], <<?[String], <<[java.sql.Date], <<[Int], <<[String], <<[String], <<[scala.math.BigDecimal], <<[scala.math.BigDecimal]))
  }
  /** Table description of table ADDRESS_SITE_GEOCODE. Objects of this class serve as prototypes for rows in queries. */
  class AddressSiteGeocode(_tableTag: Tag) extends Table[AddressSiteGeocodeRow](_tableTag, "ADDRESS_SITE_GEOCODE") {
    def * = (id.?, contribStatus, addressSiteGeocodePid, dateCreated, version, addressSitePid, geocodeTypeCode, longitude, latitude) <> (AddressSiteGeocodeRow.tupled, AddressSiteGeocodeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(contribStatus), addressSiteGeocodePid, Rep.Some(dateCreated), Rep.Some(version), Rep.Some(addressSitePid), Rep.Some(geocodeTypeCode), Rep.Some(longitude), Rep.Some(latitude)).shaped.<>({r=>import r._; _1.map(_=> AddressSiteGeocodeRow.tupled((_1, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column CONTRIB_STATUS SqlType(VARCHAR), Length(15,true) */
    val contribStatus: Rep[String] = column[String]("CONTRIB_STATUS", O.Length(15,varying=true))
    /** Database column ADDRESS_SITE_GEOCODE_PID SqlType(VARCHAR), Length(15,true) */
    val addressSiteGeocodePid: Rep[Option[String]] = column[Option[String]]("ADDRESS_SITE_GEOCODE_PID", O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column VERSION SqlType(INTEGER) */
    val version: Rep[Int] = column[Int]("VERSION")
    /** Database column ADDRESS_SITE_PID SqlType(VARCHAR), Length(15,true) */
    val addressSitePid: Rep[String] = column[String]("ADDRESS_SITE_PID", O.Length(15,varying=true))
    /** Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true) */
    val geocodeTypeCode: Rep[String] = column[String]("GEOCODE_TYPE_CODE", O.Length(4,varying=true))
    /** Database column LONGITUDE SqlType(DECIMAL) */
    val longitude: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("LONGITUDE")
    /** Database column LATITUDE SqlType(DECIMAL) */
    val latitude: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("LATITUDE")
  }
  /** Collection-like TableQuery object for table AddressSiteGeocode */
  lazy val AddressSiteGeocode = new TableQuery(tag => new AddressSiteGeocode(tag))
}
