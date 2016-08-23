package au.csiro.data61.gnaf.db

// AUTO-GENERATED Slick data model

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait GnafTables {
  val profile: slick.driver.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(AddressAlias.schema, AddressAliasTypeAut.schema, AddressDefaultGeocode.schema, AddressDetail.schema, AddressMeshBlock2011.schema, AddressSite.schema, AddressSiteGeocode.schema, AddressTypeAut.schema, AddressView.schema, FlatTypeAut.schema, GeocodedLevelTypeAut.schema, GeocodeReliabilityAut.schema, GeocodeTypeAut.schema, LevelTypeAut.schema, Locality.schema, LocalityAlias.schema, LocalityAliasTypeAut.schema, LocalityClassAut.schema, LocalityNeighbour.schema, LocalityPoint.schema, Mb2011.schema, MbMatchCodeAut.schema, PrimarySecondary.schema, PsJoinTypeAut.schema, State.schema, StreetClassAut.schema, StreetLocality.schema, StreetLocalityAlias.schema, StreetLocalityAliasTypeAut.schema, StreetLocalityPoint.schema, StreetSuffixAut.schema, StreetTypeAut.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table AddressAlias
   *  @param addressAliasPid Database column ADDRESS_ALIAS_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param principalPid Database column PRINCIPAL_PID SqlType(VARCHAR), Length(15,true)
   *  @param aliasPid Database column ALIAS_PID SqlType(VARCHAR), Length(15,true)
   *  @param aliasTypeCode Database column ALIAS_TYPE_CODE SqlType(VARCHAR), Length(10,true)
   *  @param aliasComment Database column ALIAS_COMMENT SqlType(VARCHAR), Length(200,true) */
  case class AddressAliasRow(addressAliasPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], principalPid: String, aliasPid: String, aliasTypeCode: String, aliasComment: Option[String])
  /** GetResult implicit for fetching AddressAliasRow objects using plain SQL queries */
  implicit def GetResultAddressAliasRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]]): GR[AddressAliasRow] = GR{
    prs => import prs._
    AddressAliasRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table ADDRESS_ALIAS. Objects of this class serve as prototypes for rows in queries. */
  class AddressAlias(_tableTag: Tag) extends Table[AddressAliasRow](_tableTag, "ADDRESS_ALIAS") {
    def * = (addressAliasPid, dateCreated, dateRetired, principalPid, aliasPid, aliasTypeCode, aliasComment) <> (AddressAliasRow.tupled, AddressAliasRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(addressAliasPid), Rep.Some(dateCreated), dateRetired, Rep.Some(principalPid), Rep.Some(aliasPid), Rep.Some(aliasTypeCode), aliasComment).shaped.<>({r=>import r._; _1.map(_=> AddressAliasRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ADDRESS_ALIAS_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val addressAliasPid: Rep[String] = column[String]("ADDRESS_ALIAS_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column PRINCIPAL_PID SqlType(VARCHAR), Length(15,true) */
    val principalPid: Rep[String] = column[String]("PRINCIPAL_PID", O.Length(15,varying=true))
    /** Database column ALIAS_PID SqlType(VARCHAR), Length(15,true) */
    val aliasPid: Rep[String] = column[String]("ALIAS_PID", O.Length(15,varying=true))
    /** Database column ALIAS_TYPE_CODE SqlType(VARCHAR), Length(10,true) */
    val aliasTypeCode: Rep[String] = column[String]("ALIAS_TYPE_CODE", O.Length(10,varying=true))
    /** Database column ALIAS_COMMENT SqlType(VARCHAR), Length(200,true) */
    val aliasComment: Rep[Option[String]] = column[Option[String]]("ALIAS_COMMENT", O.Length(200,varying=true))

    /** Foreign key referencing AddressAliasTypeAut (database name ADDRESS_ALIAS_FK2) */
    lazy val addressAliasTypeAutFk = foreignKey("ADDRESS_ALIAS_FK2", aliasTypeCode, AddressAliasTypeAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing AddressDetail (database name ADDRESS_ALIAS_FK1) */
    lazy val addressDetailFk2 = foreignKey("ADDRESS_ALIAS_FK1", aliasPid, AddressDetail)(r => r.addressDetailPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing AddressDetail (database name ADDRESS_ALIAS_FK3) */
    lazy val addressDetailFk3 = foreignKey("ADDRESS_ALIAS_FK3", principalPid, AddressDetail)(r => r.addressDetailPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table AddressAlias */
  lazy val AddressAlias = new TableQuery(tag => new AddressAlias(tag))

  /** Entity class storing rows of table AddressAliasTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(10,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
  case class AddressAliasTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching AddressAliasTypeAutRow objects using plain SQL queries */
  implicit def GetResultAddressAliasTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[AddressAliasTypeAutRow] = GR{
    prs => import prs._
    AddressAliasTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table ADDRESS_ALIAS_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class AddressAliasTypeAut(_tableTag: Tag) extends Table[AddressAliasTypeAutRow](_tableTag, "ADDRESS_ALIAS_TYPE_AUT") {
    def * = (code, name, description) <> (AddressAliasTypeAutRow.tupled, AddressAliasTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> AddressAliasTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(10,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(10,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(30,varying=true))
  }
  /** Collection-like TableQuery object for table AddressAliasTypeAut */
  lazy val AddressAliasTypeAut = new TableQuery(tag => new AddressAliasTypeAut(tag))

  /** Entity class storing rows of table AddressDefaultGeocode
   *  @param addressDefaultGeocodePid Database column ADDRESS_DEFAULT_GEOCODE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param addressDetailPid Database column ADDRESS_DETAIL_PID SqlType(VARCHAR), Length(15,true)
   *  @param geocodeTypeCode Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true)
   *  @param longitude Database column LONGITUDE SqlType(DECIMAL)
   *  @param latitude Database column LATITUDE SqlType(DECIMAL) */
  case class AddressDefaultGeocodeRow(addressDefaultGeocodePid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], addressDetailPid: String, geocodeTypeCode: String, longitude: Option[scala.math.BigDecimal], latitude: Option[scala.math.BigDecimal])
  /** GetResult implicit for fetching AddressDefaultGeocodeRow objects using plain SQL queries */
  implicit def GetResultAddressDefaultGeocodeRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[scala.math.BigDecimal]]): GR[AddressDefaultGeocodeRow] = GR{
    prs => import prs._
    AddressDefaultGeocodeRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<[String], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }
  /** Table description of table ADDRESS_DEFAULT_GEOCODE. Objects of this class serve as prototypes for rows in queries. */
  class AddressDefaultGeocode(_tableTag: Tag) extends Table[AddressDefaultGeocodeRow](_tableTag, "ADDRESS_DEFAULT_GEOCODE") {
    def * = (addressDefaultGeocodePid, dateCreated, dateRetired, addressDetailPid, geocodeTypeCode, longitude, latitude) <> (AddressDefaultGeocodeRow.tupled, AddressDefaultGeocodeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(addressDefaultGeocodePid), Rep.Some(dateCreated), dateRetired, Rep.Some(addressDetailPid), Rep.Some(geocodeTypeCode), longitude, latitude).shaped.<>({r=>import r._; _1.map(_=> AddressDefaultGeocodeRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ADDRESS_DEFAULT_GEOCODE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val addressDefaultGeocodePid: Rep[String] = column[String]("ADDRESS_DEFAULT_GEOCODE_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column ADDRESS_DETAIL_PID SqlType(VARCHAR), Length(15,true) */
    val addressDetailPid: Rep[String] = column[String]("ADDRESS_DETAIL_PID", O.Length(15,varying=true))
    /** Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true) */
    val geocodeTypeCode: Rep[String] = column[String]("GEOCODE_TYPE_CODE", O.Length(4,varying=true))
    /** Database column LONGITUDE SqlType(DECIMAL) */
    val longitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LONGITUDE")
    /** Database column LATITUDE SqlType(DECIMAL) */
    val latitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LATITUDE")

    /** Foreign key referencing AddressDetail (database name ADDRESS_DEFAULT_GEOCODE_FK1) */
    lazy val addressDetailFk = foreignKey("ADDRESS_DEFAULT_GEOCODE_FK1", addressDetailPid, AddressDetail)(r => r.addressDetailPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing GeocodeTypeAut (database name ADDRESS_DEFAULT_GEOCODE_FK2) */
    lazy val geocodeTypeAutFk = foreignKey("ADDRESS_DEFAULT_GEOCODE_FK2", geocodeTypeCode, GeocodeTypeAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table AddressDefaultGeocode */
  lazy val AddressDefaultGeocode = new TableQuery(tag => new AddressDefaultGeocode(tag))

  /** Row type of table AddressDetail */
  type AddressDetailRow = HCons[String,HCons[java.sql.Date,HCons[Option[java.sql.Date],HCons[Option[java.sql.Date],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[String,HCons[Option[Char],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[String,HCons[Int,HCons[Option[String],HCons[Option[String],HCons[Option[Char],HNil]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]
  /** Constructor for AddressDetailRow providing default values if available in the database schema. */
  def AddressDetailRow(addressDetailPid: String, dateCreated: java.sql.Date, dateLastModified: Option[java.sql.Date], dateRetired: Option[java.sql.Date], buildingName: Option[String], lotNumberPrefix: Option[String], lotNumber: Option[String], lotNumberSuffix: Option[String], flatTypeCode: Option[String], flatNumberPrefix: Option[String], flatNumber: Option[Int], flatNumberSuffix: Option[String], levelTypeCode: Option[String], levelNumberPrefix: Option[String], levelNumber: Option[Int], levelNumberSuffix: Option[String], numberFirstPrefix: Option[String], numberFirst: Option[Int], numberFirstSuffix: Option[String], numberLastPrefix: Option[String], numberLast: Option[Int], numberLastSuffix: Option[String], streetLocalityPid: Option[String], locationDescription: Option[String], localityPid: String, aliasPrincipal: Option[Char], postcode: Option[String], privateStreet: Option[String], legalParcelId: Option[String], confidence: Option[Int], addressSitePid: String, levelGeocodedCode: Int, propertyPid: Option[String], gnafPropertyPid: Option[String], primarySecondary: Option[Char]): AddressDetailRow = {
    addressDetailPid :: dateCreated :: dateLastModified :: dateRetired :: buildingName :: lotNumberPrefix :: lotNumber :: lotNumberSuffix :: flatTypeCode :: flatNumberPrefix :: flatNumber :: flatNumberSuffix :: levelTypeCode :: levelNumberPrefix :: levelNumber :: levelNumberSuffix :: numberFirstPrefix :: numberFirst :: numberFirstSuffix :: numberLastPrefix :: numberLast :: numberLastSuffix :: streetLocalityPid :: locationDescription :: localityPid :: aliasPrincipal :: postcode :: privateStreet :: legalParcelId :: confidence :: addressSitePid :: levelGeocodedCode :: propertyPid :: gnafPropertyPid :: primarySecondary :: HNil
  }
  /** GetResult implicit for fetching AddressDetailRow objects using plain SQL queries */
  implicit def GetResultAddressDetailRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]], e4: GR[Option[Int]], e5: GR[Option[Char]], e6: GR[Int]): GR[AddressDetailRow] = GR{
    prs => import prs._
    <<[String] :: <<[java.sql.Date] :: <<?[java.sql.Date] :: <<?[java.sql.Date] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[String] :: <<[String] :: <<?[Char] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<[String] :: <<[Int] :: <<?[String] :: <<?[String] :: <<?[Char] :: HNil
  }
  /** Table description of table ADDRESS_DETAIL. Objects of this class serve as prototypes for rows in queries. */
  class AddressDetail(_tableTag: Tag) extends Table[AddressDetailRow](_tableTag, "ADDRESS_DETAIL") {
    def * = addressDetailPid :: dateCreated :: dateLastModified :: dateRetired :: buildingName :: lotNumberPrefix :: lotNumber :: lotNumberSuffix :: flatTypeCode :: flatNumberPrefix :: flatNumber :: flatNumberSuffix :: levelTypeCode :: levelNumberPrefix :: levelNumber :: levelNumberSuffix :: numberFirstPrefix :: numberFirst :: numberFirstSuffix :: numberLastPrefix :: numberLast :: numberLastSuffix :: streetLocalityPid :: locationDescription :: localityPid :: aliasPrincipal :: postcode :: privateStreet :: legalParcelId :: confidence :: addressSitePid :: levelGeocodedCode :: propertyPid :: gnafPropertyPid :: primarySecondary :: HNil

    /** Database column ADDRESS_DETAIL_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val addressDetailPid: Rep[String] = column[String]("ADDRESS_DETAIL_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_LAST_MODIFIED SqlType(DATE) */
    val dateLastModified: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_LAST_MODIFIED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column BUILDING_NAME SqlType(VARCHAR), Length(45,true) */
    val buildingName: Rep[Option[String]] = column[Option[String]]("BUILDING_NAME", O.Length(45,varying=true))
    /** Database column LOT_NUMBER_PREFIX SqlType(VARCHAR), Length(2,true) */
    val lotNumberPrefix: Rep[Option[String]] = column[Option[String]]("LOT_NUMBER_PREFIX", O.Length(2,varying=true))
    /** Database column LOT_NUMBER SqlType(VARCHAR), Length(5,true) */
    val lotNumber: Rep[Option[String]] = column[Option[String]]("LOT_NUMBER", O.Length(5,varying=true))
    /** Database column LOT_NUMBER_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val lotNumberSuffix: Rep[Option[String]] = column[Option[String]]("LOT_NUMBER_SUFFIX", O.Length(2,varying=true))
    /** Database column FLAT_TYPE_CODE SqlType(VARCHAR), Length(7,true) */
    val flatTypeCode: Rep[Option[String]] = column[Option[String]]("FLAT_TYPE_CODE", O.Length(7,varying=true))
    /** Database column FLAT_NUMBER_PREFIX SqlType(VARCHAR), Length(2,true) */
    val flatNumberPrefix: Rep[Option[String]] = column[Option[String]]("FLAT_NUMBER_PREFIX", O.Length(2,varying=true))
    /** Database column FLAT_NUMBER SqlType(INTEGER) */
    val flatNumber: Rep[Option[Int]] = column[Option[Int]]("FLAT_NUMBER")
    /** Database column FLAT_NUMBER_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val flatNumberSuffix: Rep[Option[String]] = column[Option[String]]("FLAT_NUMBER_SUFFIX", O.Length(2,varying=true))
    /** Database column LEVEL_TYPE_CODE SqlType(VARCHAR), Length(4,true) */
    val levelTypeCode: Rep[Option[String]] = column[Option[String]]("LEVEL_TYPE_CODE", O.Length(4,varying=true))
    /** Database column LEVEL_NUMBER_PREFIX SqlType(VARCHAR), Length(2,true) */
    val levelNumberPrefix: Rep[Option[String]] = column[Option[String]]("LEVEL_NUMBER_PREFIX", O.Length(2,varying=true))
    /** Database column LEVEL_NUMBER SqlType(INTEGER) */
    val levelNumber: Rep[Option[Int]] = column[Option[Int]]("LEVEL_NUMBER")
    /** Database column LEVEL_NUMBER_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val levelNumberSuffix: Rep[Option[String]] = column[Option[String]]("LEVEL_NUMBER_SUFFIX", O.Length(2,varying=true))
    /** Database column NUMBER_FIRST_PREFIX SqlType(VARCHAR), Length(3,true) */
    val numberFirstPrefix: Rep[Option[String]] = column[Option[String]]("NUMBER_FIRST_PREFIX", O.Length(3,varying=true))
    /** Database column NUMBER_FIRST SqlType(INTEGER) */
    val numberFirst: Rep[Option[Int]] = column[Option[Int]]("NUMBER_FIRST")
    /** Database column NUMBER_FIRST_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val numberFirstSuffix: Rep[Option[String]] = column[Option[String]]("NUMBER_FIRST_SUFFIX", O.Length(2,varying=true))
    /** Database column NUMBER_LAST_PREFIX SqlType(VARCHAR), Length(3,true) */
    val numberLastPrefix: Rep[Option[String]] = column[Option[String]]("NUMBER_LAST_PREFIX", O.Length(3,varying=true))
    /** Database column NUMBER_LAST SqlType(INTEGER) */
    val numberLast: Rep[Option[Int]] = column[Option[Int]]("NUMBER_LAST")
    /** Database column NUMBER_LAST_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val numberLastSuffix: Rep[Option[String]] = column[Option[String]]("NUMBER_LAST_SUFFIX", O.Length(2,varying=true))
    /** Database column STREET_LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val streetLocalityPid: Rep[Option[String]] = column[Option[String]]("STREET_LOCALITY_PID", O.Length(15,varying=true))
    /** Database column LOCATION_DESCRIPTION SqlType(VARCHAR), Length(45,true) */
    val locationDescription: Rep[Option[String]] = column[Option[String]]("LOCATION_DESCRIPTION", O.Length(45,varying=true))
    /** Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val localityPid: Rep[String] = column[String]("LOCALITY_PID", O.Length(15,varying=true))
    /** Database column ALIAS_PRINCIPAL SqlType(CHAR) */
    val aliasPrincipal: Rep[Option[Char]] = column[Option[Char]]("ALIAS_PRINCIPAL")
    /** Database column POSTCODE SqlType(VARCHAR), Length(4,true) */
    val postcode: Rep[Option[String]] = column[Option[String]]("POSTCODE", O.Length(4,varying=true))
    /** Database column PRIVATE_STREET SqlType(VARCHAR), Length(75,true) */
    val privateStreet: Rep[Option[String]] = column[Option[String]]("PRIVATE_STREET", O.Length(75,varying=true))
    /** Database column LEGAL_PARCEL_ID SqlType(VARCHAR), Length(20,true) */
    val legalParcelId: Rep[Option[String]] = column[Option[String]]("LEGAL_PARCEL_ID", O.Length(20,varying=true))
    /** Database column CONFIDENCE SqlType(INTEGER) */
    val confidence: Rep[Option[Int]] = column[Option[Int]]("CONFIDENCE")
    /** Database column ADDRESS_SITE_PID SqlType(VARCHAR), Length(15,true) */
    val addressSitePid: Rep[String] = column[String]("ADDRESS_SITE_PID", O.Length(15,varying=true))
    /** Database column LEVEL_GEOCODED_CODE SqlType(INTEGER) */
    val levelGeocodedCode: Rep[Int] = column[Int]("LEVEL_GEOCODED_CODE")
    /** Database column PROPERTY_PID SqlType(VARCHAR), Length(15,true) */
    val propertyPid: Rep[Option[String]] = column[Option[String]]("PROPERTY_PID", O.Length(15,varying=true))
    /** Database column GNAF_PROPERTY_PID SqlType(VARCHAR), Length(15,true) */
    val gnafPropertyPid: Rep[Option[String]] = column[Option[String]]("GNAF_PROPERTY_PID", O.Length(15,varying=true))
    /** Database column PRIMARY_SECONDARY SqlType(VARCHAR) */
    val primarySecondary: Rep[Option[Char]] = column[Option[Char]]("PRIMARY_SECONDARY")

    /** Foreign key referencing AddressSite (database name ADDRESS_DETAIL_FK1) */
    lazy val addressSiteFk = foreignKey("ADDRESS_DETAIL_FK1", addressSitePid :: HNil, AddressSite)(r => r.addressSitePid :: HNil, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing FlatTypeAut (database name ADDRESS_DETAIL_FK2) */
    lazy val flatTypeAutFk = foreignKey("ADDRESS_DETAIL_FK2", flatTypeCode :: HNil, FlatTypeAut)(r => Rep.Some(r.code) :: HNil, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing GeocodedLevelTypeAut (database name ADDRESS_DETAIL_FK3) */
    lazy val geocodedLevelTypeAutFk = foreignKey("ADDRESS_DETAIL_FK3", levelGeocodedCode :: HNil, GeocodedLevelTypeAut)(r => r.code :: HNil, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing LevelTypeAut (database name ADDRESS_DETAIL_FK4) */
    lazy val levelTypeAutFk = foreignKey("ADDRESS_DETAIL_FK4", levelTypeCode :: HNil, LevelTypeAut)(r => Rep.Some(r.code) :: HNil, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Locality (database name ADDRESS_DETAIL_FK5) */
    lazy val localityFk = foreignKey("ADDRESS_DETAIL_FK5", localityPid :: HNil, Locality)(r => r.localityPid :: HNil, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing StreetLocality (database name ADDRESS_DETAIL_FK6) */
    lazy val streetLocalityFk = foreignKey("ADDRESS_DETAIL_FK6", streetLocalityPid :: HNil, StreetLocality)(r => Rep.Some(r.streetLocalityPid) :: HNil, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table AddressDetail */
  lazy val AddressDetail = new TableQuery(tag => new AddressDetail(tag))

  /** Entity class storing rows of table AddressMeshBlock2011
   *  @param addressMeshBlock2011Pid Database column ADDRESS_MESH_BLOCK_2011_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param addressDetailPid Database column ADDRESS_DETAIL_PID SqlType(VARCHAR), Length(15,true)
   *  @param mbMatchCode Database column MB_MATCH_CODE SqlType(VARCHAR), Length(15,true)
   *  @param mb2011Pid Database column MB_2011_PID SqlType(VARCHAR), Length(15,true) */
  case class AddressMeshBlock2011Row(addressMeshBlock2011Pid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], addressDetailPid: String, mbMatchCode: String, mb2011Pid: String)
  /** GetResult implicit for fetching AddressMeshBlock2011Row objects using plain SQL queries */
  implicit def GetResultAddressMeshBlock2011Row(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]]): GR[AddressMeshBlock2011Row] = GR{
    prs => import prs._
    AddressMeshBlock2011Row.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<[String], <<[String]))
  }
  /** Table description of table ADDRESS_MESH_BLOCK_2011. Objects of this class serve as prototypes for rows in queries. */
  class AddressMeshBlock2011(_tableTag: Tag) extends Table[AddressMeshBlock2011Row](_tableTag, "ADDRESS_MESH_BLOCK_2011") {
    def * = (addressMeshBlock2011Pid, dateCreated, dateRetired, addressDetailPid, mbMatchCode, mb2011Pid) <> (AddressMeshBlock2011Row.tupled, AddressMeshBlock2011Row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(addressMeshBlock2011Pid), Rep.Some(dateCreated), dateRetired, Rep.Some(addressDetailPid), Rep.Some(mbMatchCode), Rep.Some(mb2011Pid)).shaped.<>({r=>import r._; _1.map(_=> AddressMeshBlock2011Row.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ADDRESS_MESH_BLOCK_2011_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val addressMeshBlock2011Pid: Rep[String] = column[String]("ADDRESS_MESH_BLOCK_2011_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column ADDRESS_DETAIL_PID SqlType(VARCHAR), Length(15,true) */
    val addressDetailPid: Rep[String] = column[String]("ADDRESS_DETAIL_PID", O.Length(15,varying=true))
    /** Database column MB_MATCH_CODE SqlType(VARCHAR), Length(15,true) */
    val mbMatchCode: Rep[String] = column[String]("MB_MATCH_CODE", O.Length(15,varying=true))
    /** Database column MB_2011_PID SqlType(VARCHAR), Length(15,true) */
    val mb2011Pid: Rep[String] = column[String]("MB_2011_PID", O.Length(15,varying=true))

    /** Foreign key referencing AddressDetail (database name ADDRESS_MESH_BLOCK_2011_FK1) */
    lazy val addressDetailFk = foreignKey("ADDRESS_MESH_BLOCK_2011_FK1", addressDetailPid, AddressDetail)(r => r.addressDetailPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Mb2011 (database name ADDRESS_MESH_BLOCK_2011_FK2) */
    lazy val mb2011Fk = foreignKey("ADDRESS_MESH_BLOCK_2011_FK2", mb2011Pid, Mb2011)(r => r.mb2011Pid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing MbMatchCodeAut (database name ADDRESS_MESH_BLOCK_2011_FK3) */
    lazy val mbMatchCodeAutFk = foreignKey("ADDRESS_MESH_BLOCK_2011_FK3", mbMatchCode, MbMatchCodeAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table AddressMeshBlock2011 */
  lazy val AddressMeshBlock2011 = new TableQuery(tag => new AddressMeshBlock2011(tag))

  /** Entity class storing rows of table AddressSite
   *  @param addressSitePid Database column ADDRESS_SITE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param addressType Database column ADDRESS_TYPE SqlType(VARCHAR), Length(8,true)
   *  @param addressSiteName Database column ADDRESS_SITE_NAME SqlType(VARCHAR), Length(45,true) */
  case class AddressSiteRow(addressSitePid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], addressType: Option[String], addressSiteName: Option[String])
  /** GetResult implicit for fetching AddressSiteRow objects using plain SQL queries */
  implicit def GetResultAddressSiteRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]]): GR[AddressSiteRow] = GR{
    prs => import prs._
    AddressSiteRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<?[String], <<?[String]))
  }
  /** Table description of table ADDRESS_SITE. Objects of this class serve as prototypes for rows in queries. */
  class AddressSite(_tableTag: Tag) extends Table[AddressSiteRow](_tableTag, "ADDRESS_SITE") {
    def * = (addressSitePid, dateCreated, dateRetired, addressType, addressSiteName) <> (AddressSiteRow.tupled, AddressSiteRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(addressSitePid), Rep.Some(dateCreated), dateRetired, addressType, addressSiteName).shaped.<>({r=>import r._; _1.map(_=> AddressSiteRow.tupled((_1.get, _2.get, _3, _4, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ADDRESS_SITE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val addressSitePid: Rep[String] = column[String]("ADDRESS_SITE_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column ADDRESS_TYPE SqlType(VARCHAR), Length(8,true) */
    val addressType: Rep[Option[String]] = column[Option[String]]("ADDRESS_TYPE", O.Length(8,varying=true))
    /** Database column ADDRESS_SITE_NAME SqlType(VARCHAR), Length(45,true) */
    val addressSiteName: Rep[Option[String]] = column[Option[String]]("ADDRESS_SITE_NAME", O.Length(45,varying=true))

    /** Foreign key referencing AddressTypeAut (database name ADDRESS_SITE_FK1) */
    lazy val addressTypeAutFk = foreignKey("ADDRESS_SITE_FK1", addressType, AddressTypeAut)(r => Rep.Some(r.code), onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table AddressSite */
  lazy val AddressSite = new TableQuery(tag => new AddressSite(tag))

  /** Entity class storing rows of table AddressSiteGeocode
   *  @param addressSiteGeocodePid Database column ADDRESS_SITE_GEOCODE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param addressSitePid Database column ADDRESS_SITE_PID SqlType(VARCHAR), Length(15,true)
   *  @param geocodeSiteName Database column GEOCODE_SITE_NAME SqlType(VARCHAR), Length(46,true)
   *  @param geocodeSiteDescription Database column GEOCODE_SITE_DESCRIPTION SqlType(VARCHAR), Length(45,true)
   *  @param geocodeTypeCode Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true)
   *  @param reliabilityCode Database column RELIABILITY_CODE SqlType(INTEGER)
   *  @param boundaryExtent Database column BOUNDARY_EXTENT SqlType(INTEGER)
   *  @param planimetricAccuracy Database column PLANIMETRIC_ACCURACY SqlType(DECIMAL)
   *  @param elevation Database column ELEVATION SqlType(INTEGER)
   *  @param longitude Database column LONGITUDE SqlType(DECIMAL)
   *  @param latitude Database column LATITUDE SqlType(DECIMAL) */
  case class AddressSiteGeocodeRow(addressSiteGeocodePid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], addressSitePid: Option[String], geocodeSiteName: Option[String], geocodeSiteDescription: Option[String], geocodeTypeCode: Option[String], reliabilityCode: Int, boundaryExtent: Option[Int], planimetricAccuracy: Option[scala.math.BigDecimal], elevation: Option[Int], longitude: Option[scala.math.BigDecimal], latitude: Option[scala.math.BigDecimal])
  /** GetResult implicit for fetching AddressSiteGeocodeRow objects using plain SQL queries */
  implicit def GetResultAddressSiteGeocodeRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]], e4: GR[Int], e5: GR[Option[Int]], e6: GR[Option[scala.math.BigDecimal]]): GR[AddressSiteGeocodeRow] = GR{
    prs => import prs._
    AddressSiteGeocodeRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<?[String], <<?[String], <<?[String], <<?[String], <<[Int], <<?[Int], <<?[scala.math.BigDecimal], <<?[Int], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }
  /** Table description of table ADDRESS_SITE_GEOCODE. Objects of this class serve as prototypes for rows in queries. */
  class AddressSiteGeocode(_tableTag: Tag) extends Table[AddressSiteGeocodeRow](_tableTag, "ADDRESS_SITE_GEOCODE") {
    def * = (addressSiteGeocodePid, dateCreated, dateRetired, addressSitePid, geocodeSiteName, geocodeSiteDescription, geocodeTypeCode, reliabilityCode, boundaryExtent, planimetricAccuracy, elevation, longitude, latitude) <> (AddressSiteGeocodeRow.tupled, AddressSiteGeocodeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(addressSiteGeocodePid), Rep.Some(dateCreated), dateRetired, addressSitePid, geocodeSiteName, geocodeSiteDescription, geocodeTypeCode, Rep.Some(reliabilityCode), boundaryExtent, planimetricAccuracy, elevation, longitude, latitude).shaped.<>({r=>import r._; _1.map(_=> AddressSiteGeocodeRow.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8.get, _9, _10, _11, _12, _13)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ADDRESS_SITE_GEOCODE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val addressSiteGeocodePid: Rep[String] = column[String]("ADDRESS_SITE_GEOCODE_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column ADDRESS_SITE_PID SqlType(VARCHAR), Length(15,true) */
    val addressSitePid: Rep[Option[String]] = column[Option[String]]("ADDRESS_SITE_PID", O.Length(15,varying=true))
    /** Database column GEOCODE_SITE_NAME SqlType(VARCHAR), Length(46,true) */
    val geocodeSiteName: Rep[Option[String]] = column[Option[String]]("GEOCODE_SITE_NAME", O.Length(46,varying=true))
    /** Database column GEOCODE_SITE_DESCRIPTION SqlType(VARCHAR), Length(45,true) */
    val geocodeSiteDescription: Rep[Option[String]] = column[Option[String]]("GEOCODE_SITE_DESCRIPTION", O.Length(45,varying=true))
    /** Database column GEOCODE_TYPE_CODE SqlType(VARCHAR), Length(4,true) */
    val geocodeTypeCode: Rep[Option[String]] = column[Option[String]]("GEOCODE_TYPE_CODE", O.Length(4,varying=true))
    /** Database column RELIABILITY_CODE SqlType(INTEGER) */
    val reliabilityCode: Rep[Int] = column[Int]("RELIABILITY_CODE")
    /** Database column BOUNDARY_EXTENT SqlType(INTEGER) */
    val boundaryExtent: Rep[Option[Int]] = column[Option[Int]]("BOUNDARY_EXTENT")
    /** Database column PLANIMETRIC_ACCURACY SqlType(DECIMAL) */
    val planimetricAccuracy: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("PLANIMETRIC_ACCURACY")
    /** Database column ELEVATION SqlType(INTEGER) */
    val elevation: Rep[Option[Int]] = column[Option[Int]]("ELEVATION")
    /** Database column LONGITUDE SqlType(DECIMAL) */
    val longitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LONGITUDE")
    /** Database column LATITUDE SqlType(DECIMAL) */
    val latitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LATITUDE")

    /** Foreign key referencing AddressSite (database name ADDRESS_SITE_GEOCODE_FK1) */
    lazy val addressSiteFk = foreignKey("ADDRESS_SITE_GEOCODE_FK1", addressSitePid, AddressSite)(r => Rep.Some(r.addressSitePid), onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing GeocodeReliabilityAut (database name ADDRESS_SITE_GEOCODE_FK3) */
    lazy val geocodeReliabilityAutFk = foreignKey("ADDRESS_SITE_GEOCODE_FK3", reliabilityCode, GeocodeReliabilityAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing GeocodeTypeAut (database name ADDRESS_SITE_GEOCODE_FK2) */
    lazy val geocodeTypeAutFk = foreignKey("ADDRESS_SITE_GEOCODE_FK2", geocodeTypeCode, GeocodeTypeAut)(r => Rep.Some(r.code), onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table AddressSiteGeocode */
  lazy val AddressSiteGeocode = new TableQuery(tag => new AddressSiteGeocode(tag))

  /** Entity class storing rows of table AddressTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(8,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
  case class AddressTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching AddressTypeAutRow objects using plain SQL queries */
  implicit def GetResultAddressTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[AddressTypeAutRow] = GR{
    prs => import prs._
    AddressTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table ADDRESS_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class AddressTypeAut(_tableTag: Tag) extends Table[AddressTypeAutRow](_tableTag, "ADDRESS_TYPE_AUT") {
    def * = (code, name, description) <> (AddressTypeAutRow.tupled, AddressTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> AddressTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(8,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(8,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(30,varying=true))
  }
  /** Collection-like TableQuery object for table AddressTypeAut */
  lazy val AddressTypeAut = new TableQuery(tag => new AddressTypeAut(tag))

  /** Row type of table AddressView */
  type AddressViewRow = HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[Int],HCons[Option[String],HCons[Option[String],HCons[Option[Char],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[String],HCons[Option[scala.math.BigDecimal],HCons[Option[scala.math.BigDecimal],HCons[Option[String],HCons[Option[Int],HCons[Option[Char],HCons[Option[Char],HCons[Option[String],HCons[Option[java.sql.Date],HNil]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]
  /** Constructor for AddressViewRow providing default values if available in the database schema. */
  def AddressViewRow(addressDetailPid: Option[String], streetLocalityPid: Option[String], localityPid: Option[String], buildingName: Option[String], lotNumberPrefix: Option[String], lotNumber: Option[String], lotNumberSuffix: Option[String], flatType: Option[String], flatNumberPrefix: Option[String], flatNumber: Option[Int], flatNumberSuffix: Option[String], levelType: Option[String], levelNumberPrefix: Option[String], levelNumber: Option[Int], levelNumberSuffix: Option[String], numberFirstPrefix: Option[String], numberFirst: Option[Int], numberFirstSuffix: Option[String], numberLastPrefix: Option[String], numberLast: Option[Int], numberLastSuffix: Option[String], streetName: Option[String], streetClassCode: Option[Char], streetClassType: Option[String], streetTypeCode: Option[String], streetSuffixCode: Option[String], streetSuffixType: Option[String], localityName: Option[String], stateAbbreviation: Option[String], postcode: Option[String], latitude: Option[scala.math.BigDecimal], longitude: Option[scala.math.BigDecimal], geocodeType: Option[String], confidence: Option[Int], aliasPrincipal: Option[Char], primarySecondary: Option[Char], legalParcelId: Option[String], dateCreated: Option[java.sql.Date]): AddressViewRow = {
    addressDetailPid :: streetLocalityPid :: localityPid :: buildingName :: lotNumberPrefix :: lotNumber :: lotNumberSuffix :: flatType :: flatNumberPrefix :: flatNumber :: flatNumberSuffix :: levelType :: levelNumberPrefix :: levelNumber :: levelNumberSuffix :: numberFirstPrefix :: numberFirst :: numberFirstSuffix :: numberLastPrefix :: numberLast :: numberLastSuffix :: streetName :: streetClassCode :: streetClassType :: streetTypeCode :: streetSuffixCode :: streetSuffixType :: localityName :: stateAbbreviation :: postcode :: latitude :: longitude :: geocodeType :: confidence :: aliasPrincipal :: primarySecondary :: legalParcelId :: dateCreated :: HNil
  }
  /** GetResult implicit for fetching AddressViewRow objects using plain SQL queries */
  implicit def GetResultAddressViewRow(implicit e0: GR[Option[String]], e1: GR[Option[Int]], e2: GR[Option[Char]], e3: GR[Option[scala.math.BigDecimal]], e4: GR[Option[java.sql.Date]]): GR[AddressViewRow] = GR{
    prs => import prs._
    <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[Int] :: <<?[String] :: <<?[String] :: <<?[Char] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[String] :: <<?[scala.math.BigDecimal] :: <<?[scala.math.BigDecimal] :: <<?[String] :: <<?[Int] :: <<?[Char] :: <<?[Char] :: <<?[String] :: <<?[java.sql.Date] :: HNil
  }
  /** Table description of table ADDRESS_VIEW. Objects of this class serve as prototypes for rows in queries. */
  class AddressView(_tableTag: Tag) extends Table[AddressViewRow](_tableTag, "ADDRESS_VIEW") {
    def * = addressDetailPid :: streetLocalityPid :: localityPid :: buildingName :: lotNumberPrefix :: lotNumber :: lotNumberSuffix :: flatType :: flatNumberPrefix :: flatNumber :: flatNumberSuffix :: levelType :: levelNumberPrefix :: levelNumber :: levelNumberSuffix :: numberFirstPrefix :: numberFirst :: numberFirstSuffix :: numberLastPrefix :: numberLast :: numberLastSuffix :: streetName :: streetClassCode :: streetClassType :: streetTypeCode :: streetSuffixCode :: streetSuffixType :: localityName :: stateAbbreviation :: postcode :: latitude :: longitude :: geocodeType :: confidence :: aliasPrincipal :: primarySecondary :: legalParcelId :: dateCreated :: HNil

    /** Database column ADDRESS_DETAIL_PID SqlType(VARCHAR), Length(15,true) */
    val addressDetailPid: Rep[Option[String]] = column[Option[String]]("ADDRESS_DETAIL_PID", O.Length(15,varying=true))
    /** Database column STREET_LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val streetLocalityPid: Rep[Option[String]] = column[Option[String]]("STREET_LOCALITY_PID", O.Length(15,varying=true))
    /** Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val localityPid: Rep[Option[String]] = column[Option[String]]("LOCALITY_PID", O.Length(15,varying=true))
    /** Database column BUILDING_NAME SqlType(VARCHAR), Length(45,true) */
    val buildingName: Rep[Option[String]] = column[Option[String]]("BUILDING_NAME", O.Length(45,varying=true))
    /** Database column LOT_NUMBER_PREFIX SqlType(VARCHAR), Length(2,true) */
    val lotNumberPrefix: Rep[Option[String]] = column[Option[String]]("LOT_NUMBER_PREFIX", O.Length(2,varying=true))
    /** Database column LOT_NUMBER SqlType(VARCHAR), Length(5,true) */
    val lotNumber: Rep[Option[String]] = column[Option[String]]("LOT_NUMBER", O.Length(5,varying=true))
    /** Database column LOT_NUMBER_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val lotNumberSuffix: Rep[Option[String]] = column[Option[String]]("LOT_NUMBER_SUFFIX", O.Length(2,varying=true))
    /** Database column FLAT_TYPE SqlType(VARCHAR), Length(50,true) */
    val flatType: Rep[Option[String]] = column[Option[String]]("FLAT_TYPE", O.Length(50,varying=true))
    /** Database column FLAT_NUMBER_PREFIX SqlType(VARCHAR), Length(2,true) */
    val flatNumberPrefix: Rep[Option[String]] = column[Option[String]]("FLAT_NUMBER_PREFIX", O.Length(2,varying=true))
    /** Database column FLAT_NUMBER SqlType(INTEGER) */
    val flatNumber: Rep[Option[Int]] = column[Option[Int]]("FLAT_NUMBER")
    /** Database column FLAT_NUMBER_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val flatNumberSuffix: Rep[Option[String]] = column[Option[String]]("FLAT_NUMBER_SUFFIX", O.Length(2,varying=true))
    /** Database column LEVEL_TYPE SqlType(VARCHAR), Length(50,true) */
    val levelType: Rep[Option[String]] = column[Option[String]]("LEVEL_TYPE", O.Length(50,varying=true))
    /** Database column LEVEL_NUMBER_PREFIX SqlType(VARCHAR), Length(2,true) */
    val levelNumberPrefix: Rep[Option[String]] = column[Option[String]]("LEVEL_NUMBER_PREFIX", O.Length(2,varying=true))
    /** Database column LEVEL_NUMBER SqlType(INTEGER) */
    val levelNumber: Rep[Option[Int]] = column[Option[Int]]("LEVEL_NUMBER")
    /** Database column LEVEL_NUMBER_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val levelNumberSuffix: Rep[Option[String]] = column[Option[String]]("LEVEL_NUMBER_SUFFIX", O.Length(2,varying=true))
    /** Database column NUMBER_FIRST_PREFIX SqlType(VARCHAR), Length(3,true) */
    val numberFirstPrefix: Rep[Option[String]] = column[Option[String]]("NUMBER_FIRST_PREFIX", O.Length(3,varying=true))
    /** Database column NUMBER_FIRST SqlType(INTEGER) */
    val numberFirst: Rep[Option[Int]] = column[Option[Int]]("NUMBER_FIRST")
    /** Database column NUMBER_FIRST_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val numberFirstSuffix: Rep[Option[String]] = column[Option[String]]("NUMBER_FIRST_SUFFIX", O.Length(2,varying=true))
    /** Database column NUMBER_LAST_PREFIX SqlType(VARCHAR), Length(3,true) */
    val numberLastPrefix: Rep[Option[String]] = column[Option[String]]("NUMBER_LAST_PREFIX", O.Length(3,varying=true))
    /** Database column NUMBER_LAST SqlType(INTEGER) */
    val numberLast: Rep[Option[Int]] = column[Option[Int]]("NUMBER_LAST")
    /** Database column NUMBER_LAST_SUFFIX SqlType(VARCHAR), Length(2,true) */
    val numberLastSuffix: Rep[Option[String]] = column[Option[String]]("NUMBER_LAST_SUFFIX", O.Length(2,varying=true))
    /** Database column STREET_NAME SqlType(VARCHAR), Length(100,true) */
    val streetName: Rep[Option[String]] = column[Option[String]]("STREET_NAME", O.Length(100,varying=true))
    /** Database column STREET_CLASS_CODE SqlType(CHAR) */
    val streetClassCode: Rep[Option[Char]] = column[Option[Char]]("STREET_CLASS_CODE")
    /** Database column STREET_CLASS_TYPE SqlType(VARCHAR), Length(50,true) */
    val streetClassType: Rep[Option[String]] = column[Option[String]]("STREET_CLASS_TYPE", O.Length(50,varying=true))
    /** Database column STREET_TYPE_CODE SqlType(VARCHAR), Length(15,true) */
    val streetTypeCode: Rep[Option[String]] = column[Option[String]]("STREET_TYPE_CODE", O.Length(15,varying=true))
    /** Database column STREET_SUFFIX_CODE SqlType(VARCHAR), Length(15,true) */
    val streetSuffixCode: Rep[Option[String]] = column[Option[String]]("STREET_SUFFIX_CODE", O.Length(15,varying=true))
    /** Database column STREET_SUFFIX_TYPE SqlType(VARCHAR), Length(50,true) */
    val streetSuffixType: Rep[Option[String]] = column[Option[String]]("STREET_SUFFIX_TYPE", O.Length(50,varying=true))
    /** Database column LOCALITY_NAME SqlType(VARCHAR), Length(100,true) */
    val localityName: Rep[Option[String]] = column[Option[String]]("LOCALITY_NAME", O.Length(100,varying=true))
    /** Database column STATE_ABBREVIATION SqlType(VARCHAR), Length(3,true) */
    val stateAbbreviation: Rep[Option[String]] = column[Option[String]]("STATE_ABBREVIATION", O.Length(3,varying=true))
    /** Database column POSTCODE SqlType(VARCHAR), Length(4,true) */
    val postcode: Rep[Option[String]] = column[Option[String]]("POSTCODE", O.Length(4,varying=true))
    /** Database column LATITUDE SqlType(DECIMAL) */
    val latitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LATITUDE")
    /** Database column LONGITUDE SqlType(DECIMAL) */
    val longitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LONGITUDE")
    /** Database column GEOCODE_TYPE SqlType(VARCHAR), Length(50,true) */
    val geocodeType: Rep[Option[String]] = column[Option[String]]("GEOCODE_TYPE", O.Length(50,varying=true))
    /** Database column CONFIDENCE SqlType(INTEGER) */
    val confidence: Rep[Option[Int]] = column[Option[Int]]("CONFIDENCE")
    /** Database column ALIAS_PRINCIPAL SqlType(CHAR) */
    val aliasPrincipal: Rep[Option[Char]] = column[Option[Char]]("ALIAS_PRINCIPAL")
    /** Database column PRIMARY_SECONDARY SqlType(VARCHAR) */
    val primarySecondary: Rep[Option[Char]] = column[Option[Char]]("PRIMARY_SECONDARY")
    /** Database column LEGAL_PARCEL_ID SqlType(VARCHAR), Length(20,true) */
    val legalParcelId: Rep[Option[String]] = column[Option[String]]("LEGAL_PARCEL_ID", O.Length(20,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_CREATED")
  }
  /** Collection-like TableQuery object for table AddressView */
  lazy val AddressView = new TableQuery(tag => new AddressView(tag))

  /** Entity class storing rows of table FlatTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(7,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
  case class FlatTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching FlatTypeAutRow objects using plain SQL queries */
  implicit def GetResultFlatTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[FlatTypeAutRow] = GR{
    prs => import prs._
    FlatTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table FLAT_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class FlatTypeAut(_tableTag: Tag) extends Table[FlatTypeAutRow](_tableTag, "FLAT_TYPE_AUT") {
    def * = (code, name, description) <> (FlatTypeAutRow.tupled, FlatTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> FlatTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(7,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(7,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(30,varying=true))
  }
  /** Collection-like TableQuery object for table FlatTypeAut */
  lazy val FlatTypeAut = new TableQuery(tag => new FlatTypeAut(tag))

  /** Entity class storing rows of table GeocodedLevelTypeAut
   *  @param code Database column CODE SqlType(INTEGER), PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(70,true) */
  case class GeocodedLevelTypeAutRow(code: Int, name: String, description: Option[String])
  /** GetResult implicit for fetching GeocodedLevelTypeAutRow objects using plain SQL queries */
  implicit def GetResultGeocodedLevelTypeAutRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[GeocodedLevelTypeAutRow] = GR{
    prs => import prs._
    GeocodedLevelTypeAutRow.tupled((<<[Int], <<[String], <<?[String]))
  }
  /** Table description of table GEOCODED_LEVEL_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class GeocodedLevelTypeAut(_tableTag: Tag) extends Table[GeocodedLevelTypeAutRow](_tableTag, "GEOCODED_LEVEL_TYPE_AUT") {
    def * = (code, name, description) <> (GeocodedLevelTypeAutRow.tupled, GeocodedLevelTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> GeocodedLevelTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(INTEGER), PrimaryKey */
    val code: Rep[Int] = column[Int]("CODE", O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(70,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(70,varying=true))
  }
  /** Collection-like TableQuery object for table GeocodedLevelTypeAut */
  lazy val GeocodedLevelTypeAut = new TableQuery(tag => new GeocodedLevelTypeAut(tag))

  /** Entity class storing rows of table GeocodeReliabilityAut
   *  @param code Database column CODE SqlType(INTEGER), PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(100,true) */
  case class GeocodeReliabilityAutRow(code: Int, name: String, description: Option[String])
  /** GetResult implicit for fetching GeocodeReliabilityAutRow objects using plain SQL queries */
  implicit def GetResultGeocodeReliabilityAutRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[GeocodeReliabilityAutRow] = GR{
    prs => import prs._
    GeocodeReliabilityAutRow.tupled((<<[Int], <<[String], <<?[String]))
  }
  /** Table description of table GEOCODE_RELIABILITY_AUT. Objects of this class serve as prototypes for rows in queries. */
  class GeocodeReliabilityAut(_tableTag: Tag) extends Table[GeocodeReliabilityAutRow](_tableTag, "GEOCODE_RELIABILITY_AUT") {
    def * = (code, name, description) <> (GeocodeReliabilityAutRow.tupled, GeocodeReliabilityAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> GeocodeReliabilityAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(INTEGER), PrimaryKey */
    val code: Rep[Int] = column[Int]("CODE", O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(100,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(100,varying=true))
  }
  /** Collection-like TableQuery object for table GeocodeReliabilityAut */
  lazy val GeocodeReliabilityAut = new TableQuery(tag => new GeocodeReliabilityAut(tag))

  /** Entity class storing rows of table GeocodeTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(4,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(250,true) */
  case class GeocodeTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching GeocodeTypeAutRow objects using plain SQL queries */
  implicit def GetResultGeocodeTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[GeocodeTypeAutRow] = GR{
    prs => import prs._
    GeocodeTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table GEOCODE_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class GeocodeTypeAut(_tableTag: Tag) extends Table[GeocodeTypeAutRow](_tableTag, "GEOCODE_TYPE_AUT") {
    def * = (code, name, description) <> (GeocodeTypeAutRow.tupled, GeocodeTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> GeocodeTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(4,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(4,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(250,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(250,varying=true))
  }
  /** Collection-like TableQuery object for table GeocodeTypeAut */
  lazy val GeocodeTypeAut = new TableQuery(tag => new GeocodeTypeAut(tag))

  /** Entity class storing rows of table LevelTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(4,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
  case class LevelTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching LevelTypeAutRow objects using plain SQL queries */
  implicit def GetResultLevelTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[LevelTypeAutRow] = GR{
    prs => import prs._
    LevelTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table LEVEL_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class LevelTypeAut(_tableTag: Tag) extends Table[LevelTypeAutRow](_tableTag, "LEVEL_TYPE_AUT") {
    def * = (code, name, description) <> (LevelTypeAutRow.tupled, LevelTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> LevelTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(4,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(4,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(30,varying=true))
  }
  /** Collection-like TableQuery object for table LevelTypeAut */
  lazy val LevelTypeAut = new TableQuery(tag => new LevelTypeAut(tag))

  /** Entity class storing rows of table Locality
   *  @param localityPid Database column LOCALITY_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param localityName Database column LOCALITY_NAME SqlType(VARCHAR), Length(100,true)
   *  @param primaryPostcode Database column PRIMARY_POSTCODE SqlType(VARCHAR), Length(4,true)
   *  @param localityClassCode Database column LOCALITY_CLASS_CODE SqlType(CHAR)
   *  @param statePid Database column STATE_PID SqlType(VARCHAR), Length(15,true)
   *  @param gnafLocalityPid Database column GNAF_LOCALITY_PID SqlType(VARCHAR), Length(15,true)
   *  @param gnafReliabilityCode Database column GNAF_RELIABILITY_CODE SqlType(INTEGER) */
  case class LocalityRow(localityPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], localityName: String, primaryPostcode: Option[String], localityClassCode: Char, statePid: String, gnafLocalityPid: Option[String], gnafReliabilityCode: Int)
  /** GetResult implicit for fetching LocalityRow objects using plain SQL queries */
  implicit def GetResultLocalityRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]], e4: GR[Char], e5: GR[Int]): GR[LocalityRow] = GR{
    prs => import prs._
    LocalityRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<?[String], <<[Char], <<[String], <<?[String], <<[Int]))
  }
  /** Table description of table LOCALITY. Objects of this class serve as prototypes for rows in queries. */
  class Locality(_tableTag: Tag) extends Table[LocalityRow](_tableTag, "LOCALITY") {
    def * = (localityPid, dateCreated, dateRetired, localityName, primaryPostcode, localityClassCode, statePid, gnafLocalityPid, gnafReliabilityCode) <> (LocalityRow.tupled, LocalityRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(localityPid), Rep.Some(dateCreated), dateRetired, Rep.Some(localityName), primaryPostcode, Rep.Some(localityClassCode), Rep.Some(statePid), gnafLocalityPid, Rep.Some(gnafReliabilityCode)).shaped.<>({r=>import r._; _1.map(_=> LocalityRow.tupled((_1.get, _2.get, _3, _4.get, _5, _6.get, _7.get, _8, _9.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column LOCALITY_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val localityPid: Rep[String] = column[String]("LOCALITY_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column LOCALITY_NAME SqlType(VARCHAR), Length(100,true) */
    val localityName: Rep[String] = column[String]("LOCALITY_NAME", O.Length(100,varying=true))
    /** Database column PRIMARY_POSTCODE SqlType(VARCHAR), Length(4,true) */
    val primaryPostcode: Rep[Option[String]] = column[Option[String]]("PRIMARY_POSTCODE", O.Length(4,varying=true))
    /** Database column LOCALITY_CLASS_CODE SqlType(CHAR) */
    val localityClassCode: Rep[Char] = column[Char]("LOCALITY_CLASS_CODE")
    /** Database column STATE_PID SqlType(VARCHAR), Length(15,true) */
    val statePid: Rep[String] = column[String]("STATE_PID", O.Length(15,varying=true))
    /** Database column GNAF_LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val gnafLocalityPid: Rep[Option[String]] = column[Option[String]]("GNAF_LOCALITY_PID", O.Length(15,varying=true))
    /** Database column GNAF_RELIABILITY_CODE SqlType(INTEGER) */
    val gnafReliabilityCode: Rep[Int] = column[Int]("GNAF_RELIABILITY_CODE")

    /** Foreign key referencing GeocodeReliabilityAut (database name LOCALITY_FK1) */
    lazy val geocodeReliabilityAutFk = foreignKey("LOCALITY_FK1", gnafReliabilityCode, GeocodeReliabilityAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing LocalityClassAut (database name LOCALITY_FK2) */
    lazy val localityClassAutFk = foreignKey("LOCALITY_FK2", localityClassCode, LocalityClassAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing State (database name LOCALITY_FK3) */
    lazy val stateFk = foreignKey("LOCALITY_FK3", statePid, State)(r => r.statePid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table Locality */
  lazy val Locality = new TableQuery(tag => new Locality(tag))

  /** Entity class storing rows of table LocalityAlias
   *  @param localityAliasPid Database column LOCALITY_ALIAS_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param localityPid Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(100,true)
   *  @param postcode Database column POSTCODE SqlType(VARCHAR), Length(4,true)
   *  @param aliasTypeCode Database column ALIAS_TYPE_CODE SqlType(VARCHAR), Length(10,true)
   *  @param statePid Database column STATE_PID SqlType(VARCHAR), Length(15,true) */
  case class LocalityAliasRow(localityAliasPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], localityPid: String, name: String, postcode: Option[String], aliasTypeCode: String, statePid: String)
  /** GetResult implicit for fetching LocalityAliasRow objects using plain SQL queries */
  implicit def GetResultLocalityAliasRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]]): GR[LocalityAliasRow] = GR{
    prs => import prs._
    LocalityAliasRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<[String], <<?[String], <<[String], <<[String]))
  }
  /** Table description of table LOCALITY_ALIAS. Objects of this class serve as prototypes for rows in queries. */
  class LocalityAlias(_tableTag: Tag) extends Table[LocalityAliasRow](_tableTag, "LOCALITY_ALIAS") {
    def * = (localityAliasPid, dateCreated, dateRetired, localityPid, name, postcode, aliasTypeCode, statePid) <> (LocalityAliasRow.tupled, LocalityAliasRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(localityAliasPid), Rep.Some(dateCreated), dateRetired, Rep.Some(localityPid), Rep.Some(name), postcode, Rep.Some(aliasTypeCode), Rep.Some(statePid)).shaped.<>({r=>import r._; _1.map(_=> LocalityAliasRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column LOCALITY_ALIAS_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val localityAliasPid: Rep[String] = column[String]("LOCALITY_ALIAS_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val localityPid: Rep[String] = column[String]("LOCALITY_PID", O.Length(15,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(100,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column POSTCODE SqlType(VARCHAR), Length(4,true) */
    val postcode: Rep[Option[String]] = column[Option[String]]("POSTCODE", O.Length(4,varying=true))
    /** Database column ALIAS_TYPE_CODE SqlType(VARCHAR), Length(10,true) */
    val aliasTypeCode: Rep[String] = column[String]("ALIAS_TYPE_CODE", O.Length(10,varying=true))
    /** Database column STATE_PID SqlType(VARCHAR), Length(15,true) */
    val statePid: Rep[String] = column[String]("STATE_PID", O.Length(15,varying=true))

    /** Foreign key referencing Locality (database name LOCALITY_ALIAS_FK2) */
    lazy val localityFk = foreignKey("LOCALITY_ALIAS_FK2", localityPid, Locality)(r => r.localityPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing LocalityAliasTypeAut (database name LOCALITY_ALIAS_FK1) */
    lazy val localityAliasTypeAutFk = foreignKey("LOCALITY_ALIAS_FK1", aliasTypeCode, LocalityAliasTypeAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table LocalityAlias */
  lazy val LocalityAlias = new TableQuery(tag => new LocalityAlias(tag))

  /** Entity class storing rows of table LocalityAliasTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(10,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(100,true) */
  case class LocalityAliasTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching LocalityAliasTypeAutRow objects using plain SQL queries */
  implicit def GetResultLocalityAliasTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[LocalityAliasTypeAutRow] = GR{
    prs => import prs._
    LocalityAliasTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table LOCALITY_ALIAS_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class LocalityAliasTypeAut(_tableTag: Tag) extends Table[LocalityAliasTypeAutRow](_tableTag, "LOCALITY_ALIAS_TYPE_AUT") {
    def * = (code, name, description) <> (LocalityAliasTypeAutRow.tupled, LocalityAliasTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> LocalityAliasTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(10,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(10,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(100,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(100,varying=true))
  }
  /** Collection-like TableQuery object for table LocalityAliasTypeAut */
  lazy val LocalityAliasTypeAut = new TableQuery(tag => new LocalityAliasTypeAut(tag))

  /** Entity class storing rows of table LocalityClassAut
   *  @param code Database column CODE SqlType(CHAR), PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(200,true) */
  case class LocalityClassAutRow(code: Char, name: String, description: Option[String])
  /** GetResult implicit for fetching LocalityClassAutRow objects using plain SQL queries */
  implicit def GetResultLocalityClassAutRow(implicit e0: GR[Char], e1: GR[String], e2: GR[Option[String]]): GR[LocalityClassAutRow] = GR{
    prs => import prs._
    LocalityClassAutRow.tupled((<<[Char], <<[String], <<?[String]))
  }
  /** Table description of table LOCALITY_CLASS_AUT. Objects of this class serve as prototypes for rows in queries. */
  class LocalityClassAut(_tableTag: Tag) extends Table[LocalityClassAutRow](_tableTag, "LOCALITY_CLASS_AUT") {
    def * = (code, name, description) <> (LocalityClassAutRow.tupled, LocalityClassAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> LocalityClassAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(CHAR), PrimaryKey */
    val code: Rep[Char] = column[Char]("CODE", O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(200,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(200,varying=true))
  }
  /** Collection-like TableQuery object for table LocalityClassAut */
  lazy val LocalityClassAut = new TableQuery(tag => new LocalityClassAut(tag))

  /** Entity class storing rows of table LocalityNeighbour
   *  @param localityNeighbourPid Database column LOCALITY_NEIGHBOUR_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param localityPid Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true)
   *  @param neighbourLocalityPid Database column NEIGHBOUR_LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
  case class LocalityNeighbourRow(localityNeighbourPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], localityPid: String, neighbourLocalityPid: String)
  /** GetResult implicit for fetching LocalityNeighbourRow objects using plain SQL queries */
  implicit def GetResultLocalityNeighbourRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]]): GR[LocalityNeighbourRow] = GR{
    prs => import prs._
    LocalityNeighbourRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<[String]))
  }
  /** Table description of table LOCALITY_NEIGHBOUR. Objects of this class serve as prototypes for rows in queries. */
  class LocalityNeighbour(_tableTag: Tag) extends Table[LocalityNeighbourRow](_tableTag, "LOCALITY_NEIGHBOUR") {
    def * = (localityNeighbourPid, dateCreated, dateRetired, localityPid, neighbourLocalityPid) <> (LocalityNeighbourRow.tupled, LocalityNeighbourRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(localityNeighbourPid), Rep.Some(dateCreated), dateRetired, Rep.Some(localityPid), Rep.Some(neighbourLocalityPid)).shaped.<>({r=>import r._; _1.map(_=> LocalityNeighbourRow.tupled((_1.get, _2.get, _3, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column LOCALITY_NEIGHBOUR_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val localityNeighbourPid: Rep[String] = column[String]("LOCALITY_NEIGHBOUR_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val localityPid: Rep[String] = column[String]("LOCALITY_PID", O.Length(15,varying=true))
    /** Database column NEIGHBOUR_LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val neighbourLocalityPid: Rep[String] = column[String]("NEIGHBOUR_LOCALITY_PID", O.Length(15,varying=true))

    /** Foreign key referencing Locality (database name LOCALITY_NEIGHBOUR_FK1) */
    lazy val localityFk1 = foreignKey("LOCALITY_NEIGHBOUR_FK1", localityPid, Locality)(r => r.localityPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Locality (database name LOCALITY_NEIGHBOUR_FK2) */
    lazy val localityFk2 = foreignKey("LOCALITY_NEIGHBOUR_FK2", neighbourLocalityPid, Locality)(r => r.localityPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table LocalityNeighbour */
  lazy val LocalityNeighbour = new TableQuery(tag => new LocalityNeighbour(tag))

  /** Entity class storing rows of table LocalityPoint
   *  @param localityPointPid Database column LOCALITY_POINT_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param localityPid Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true)
   *  @param planimetricAccuracy Database column PLANIMETRIC_ACCURACY SqlType(DECIMAL)
   *  @param longitude Database column LONGITUDE SqlType(DECIMAL)
   *  @param latitude Database column LATITUDE SqlType(DECIMAL) */
  case class LocalityPointRow(localityPointPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], localityPid: String, planimetricAccuracy: Option[scala.math.BigDecimal], longitude: Option[scala.math.BigDecimal], latitude: Option[scala.math.BigDecimal])
  /** GetResult implicit for fetching LocalityPointRow objects using plain SQL queries */
  implicit def GetResultLocalityPointRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[scala.math.BigDecimal]]): GR[LocalityPointRow] = GR{
    prs => import prs._
    LocalityPointRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }
  /** Table description of table LOCALITY_POINT. Objects of this class serve as prototypes for rows in queries. */
  class LocalityPoint(_tableTag: Tag) extends Table[LocalityPointRow](_tableTag, "LOCALITY_POINT") {
    def * = (localityPointPid, dateCreated, dateRetired, localityPid, planimetricAccuracy, longitude, latitude) <> (LocalityPointRow.tupled, LocalityPointRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(localityPointPid), Rep.Some(dateCreated), dateRetired, Rep.Some(localityPid), planimetricAccuracy, longitude, latitude).shaped.<>({r=>import r._; _1.map(_=> LocalityPointRow.tupled((_1.get, _2.get, _3, _4.get, _5, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column LOCALITY_POINT_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val localityPointPid: Rep[String] = column[String]("LOCALITY_POINT_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val localityPid: Rep[String] = column[String]("LOCALITY_PID", O.Length(15,varying=true))
    /** Database column PLANIMETRIC_ACCURACY SqlType(DECIMAL) */
    val planimetricAccuracy: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("PLANIMETRIC_ACCURACY")
    /** Database column LONGITUDE SqlType(DECIMAL) */
    val longitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LONGITUDE")
    /** Database column LATITUDE SqlType(DECIMAL) */
    val latitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LATITUDE")

    /** Foreign key referencing Locality (database name LOCALITY_POINT_FK1) */
    lazy val localityFk = foreignKey("LOCALITY_POINT_FK1", localityPid, Locality)(r => r.localityPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table LocalityPoint */
  lazy val LocalityPoint = new TableQuery(tag => new LocalityPoint(tag))

  /** Entity class storing rows of table Mb2011
   *  @param mb2011Pid Database column MB_2011_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param mb2011Code Database column MB_2011_CODE SqlType(VARCHAR), Length(15,true) */
  case class Mb2011Row(mb2011Pid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], mb2011Code: String)
  /** GetResult implicit for fetching Mb2011Row objects using plain SQL queries */
  implicit def GetResultMb2011Row(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]]): GR[Mb2011Row] = GR{
    prs => import prs._
    Mb2011Row.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String]))
  }
  /** Table description of table MB_2011. Objects of this class serve as prototypes for rows in queries. */
  class Mb2011(_tableTag: Tag) extends Table[Mb2011Row](_tableTag, "MB_2011") {
    def * = (mb2011Pid, dateCreated, dateRetired, mb2011Code) <> (Mb2011Row.tupled, Mb2011Row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(mb2011Pid), Rep.Some(dateCreated), dateRetired, Rep.Some(mb2011Code)).shaped.<>({r=>import r._; _1.map(_=> Mb2011Row.tupled((_1.get, _2.get, _3, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column MB_2011_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val mb2011Pid: Rep[String] = column[String]("MB_2011_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column MB_2011_CODE SqlType(VARCHAR), Length(15,true) */
    val mb2011Code: Rep[String] = column[String]("MB_2011_CODE", O.Length(15,varying=true))
  }
  /** Collection-like TableQuery object for table Mb2011 */
  lazy val Mb2011 = new TableQuery(tag => new Mb2011(tag))

  /** Entity class storing rows of table MbMatchCodeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(100,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(250,true) */
  case class MbMatchCodeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching MbMatchCodeAutRow objects using plain SQL queries */
  implicit def GetResultMbMatchCodeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[MbMatchCodeAutRow] = GR{
    prs => import prs._
    MbMatchCodeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table MB_MATCH_CODE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class MbMatchCodeAut(_tableTag: Tag) extends Table[MbMatchCodeAutRow](_tableTag, "MB_MATCH_CODE_AUT") {
    def * = (code, name, description) <> (MbMatchCodeAutRow.tupled, MbMatchCodeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> MbMatchCodeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(100,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(250,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(250,varying=true))
  }
  /** Collection-like TableQuery object for table MbMatchCodeAut */
  lazy val MbMatchCodeAut = new TableQuery(tag => new MbMatchCodeAut(tag))

  /** Entity class storing rows of table PrimarySecondary
   *  @param primarySecondaryPid Database column PRIMARY_SECONDARY_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param primaryPid Database column PRIMARY_PID SqlType(VARCHAR), Length(15,true)
   *  @param secondaryPid Database column SECONDARY_PID SqlType(VARCHAR), Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param psJoinTypeCode Database column PS_JOIN_TYPE_CODE SqlType(INTEGER)
   *  @param psJoinComment Database column PS_JOIN_COMMENT SqlType(VARCHAR), Length(500,true) */
  case class PrimarySecondaryRow(primarySecondaryPid: String, primaryPid: String, secondaryPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], psJoinTypeCode: Int, psJoinComment: Option[String])
  /** GetResult implicit for fetching PrimarySecondaryRow objects using plain SQL queries */
  implicit def GetResultPrimarySecondaryRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Int], e4: GR[Option[String]]): GR[PrimarySecondaryRow] = GR{
    prs => import prs._
    PrimarySecondaryRow.tupled((<<[String], <<[String], <<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[Int], <<?[String]))
  }
  /** Table description of table PRIMARY_SECONDARY. Objects of this class serve as prototypes for rows in queries. */
  class PrimarySecondary(_tableTag: Tag) extends Table[PrimarySecondaryRow](_tableTag, "PRIMARY_SECONDARY") {
    def * = (primarySecondaryPid, primaryPid, secondaryPid, dateCreated, dateRetired, psJoinTypeCode, psJoinComment) <> (PrimarySecondaryRow.tupled, PrimarySecondaryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(primarySecondaryPid), Rep.Some(primaryPid), Rep.Some(secondaryPid), Rep.Some(dateCreated), dateRetired, Rep.Some(psJoinTypeCode), psJoinComment).shaped.<>({r=>import r._; _1.map(_=> PrimarySecondaryRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column PRIMARY_SECONDARY_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val primarySecondaryPid: Rep[String] = column[String]("PRIMARY_SECONDARY_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column PRIMARY_PID SqlType(VARCHAR), Length(15,true) */
    val primaryPid: Rep[String] = column[String]("PRIMARY_PID", O.Length(15,varying=true))
    /** Database column SECONDARY_PID SqlType(VARCHAR), Length(15,true) */
    val secondaryPid: Rep[String] = column[String]("SECONDARY_PID", O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column PS_JOIN_TYPE_CODE SqlType(INTEGER) */
    val psJoinTypeCode: Rep[Int] = column[Int]("PS_JOIN_TYPE_CODE")
    /** Database column PS_JOIN_COMMENT SqlType(VARCHAR), Length(500,true) */
    val psJoinComment: Rep[Option[String]] = column[Option[String]]("PS_JOIN_COMMENT", O.Length(500,varying=true))

    /** Foreign key referencing AddressDetail (database name PRIMARY_SECONDARY_FK1) */
    lazy val addressDetailFk1 = foreignKey("PRIMARY_SECONDARY_FK1", primaryPid, AddressDetail)(r => r.addressDetailPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing AddressDetail (database name PRIMARY_SECONDARY_FK3) */
    lazy val addressDetailFk2 = foreignKey("PRIMARY_SECONDARY_FK3", secondaryPid, AddressDetail)(r => r.addressDetailPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing PsJoinTypeAut (database name PRIMARY_SECONDARY_FK2) */
    lazy val psJoinTypeAutFk = foreignKey("PRIMARY_SECONDARY_FK2", psJoinTypeCode, PsJoinTypeAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table PrimarySecondary */
  lazy val PrimarySecondary = new TableQuery(tag => new PrimarySecondary(tag))

  /** Entity class storing rows of table PsJoinTypeAut
   *  @param code Database column CODE SqlType(INTEGER), PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(500,true) */
  case class PsJoinTypeAutRow(code: Int, name: String, description: Option[String])
  /** GetResult implicit for fetching PsJoinTypeAutRow objects using plain SQL queries */
  implicit def GetResultPsJoinTypeAutRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[PsJoinTypeAutRow] = GR{
    prs => import prs._
    PsJoinTypeAutRow.tupled((<<[Int], <<[String], <<?[String]))
  }
  /** Table description of table PS_JOIN_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class PsJoinTypeAut(_tableTag: Tag) extends Table[PsJoinTypeAutRow](_tableTag, "PS_JOIN_TYPE_AUT") {
    def * = (code, name, description) <> (PsJoinTypeAutRow.tupled, PsJoinTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> PsJoinTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(INTEGER), PrimaryKey */
    val code: Rep[Int] = column[Int]("CODE", O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(500,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(500,varying=true))
  }
  /** Collection-like TableQuery object for table PsJoinTypeAut */
  lazy val PsJoinTypeAut = new TableQuery(tag => new PsJoinTypeAut(tag))

  /** Entity class storing rows of table State
   *  @param statePid Database column STATE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param stateName Database column STATE_NAME SqlType(VARCHAR), Length(50,true)
   *  @param stateAbbreviation Database column STATE_ABBREVIATION SqlType(VARCHAR), Length(3,true) */
  case class StateRow(statePid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], stateName: String, stateAbbreviation: String)
  /** GetResult implicit for fetching StateRow objects using plain SQL queries */
  implicit def GetResultStateRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]]): GR[StateRow] = GR{
    prs => import prs._
    StateRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<[String]))
  }
  /** Table description of table STATE. Objects of this class serve as prototypes for rows in queries. */
  class State(_tableTag: Tag) extends Table[StateRow](_tableTag, "STATE") {
    def * = (statePid, dateCreated, dateRetired, stateName, stateAbbreviation) <> (StateRow.tupled, StateRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(statePid), Rep.Some(dateCreated), dateRetired, Rep.Some(stateName), Rep.Some(stateAbbreviation)).shaped.<>({r=>import r._; _1.map(_=> StateRow.tupled((_1.get, _2.get, _3, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column STATE_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val statePid: Rep[String] = column[String]("STATE_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column STATE_NAME SqlType(VARCHAR), Length(50,true) */
    val stateName: Rep[String] = column[String]("STATE_NAME", O.Length(50,varying=true))
    /** Database column STATE_ABBREVIATION SqlType(VARCHAR), Length(3,true) */
    val stateAbbreviation: Rep[String] = column[String]("STATE_ABBREVIATION", O.Length(3,varying=true))
  }
  /** Collection-like TableQuery object for table State */
  lazy val State = new TableQuery(tag => new State(tag))

  /** Entity class storing rows of table StreetClassAut
   *  @param code Database column CODE SqlType(CHAR), PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(200,true) */
  case class StreetClassAutRow(code: Char, name: String, description: Option[String])
  /** GetResult implicit for fetching StreetClassAutRow objects using plain SQL queries */
  implicit def GetResultStreetClassAutRow(implicit e0: GR[Char], e1: GR[String], e2: GR[Option[String]]): GR[StreetClassAutRow] = GR{
    prs => import prs._
    StreetClassAutRow.tupled((<<[Char], <<[String], <<?[String]))
  }
  /** Table description of table STREET_CLASS_AUT. Objects of this class serve as prototypes for rows in queries. */
  class StreetClassAut(_tableTag: Tag) extends Table[StreetClassAutRow](_tableTag, "STREET_CLASS_AUT") {
    def * = (code, name, description) <> (StreetClassAutRow.tupled, StreetClassAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> StreetClassAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(CHAR), PrimaryKey */
    val code: Rep[Char] = column[Char]("CODE", O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(200,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(200,varying=true))
  }
  /** Collection-like TableQuery object for table StreetClassAut */
  lazy val StreetClassAut = new TableQuery(tag => new StreetClassAut(tag))

  /** Entity class storing rows of table StreetLocality
   *  @param streetLocalityPid Database column STREET_LOCALITY_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param streetClassCode Database column STREET_CLASS_CODE SqlType(CHAR)
   *  @param streetName Database column STREET_NAME SqlType(VARCHAR), Length(100,true)
   *  @param streetTypeCode Database column STREET_TYPE_CODE SqlType(VARCHAR), Length(15,true)
   *  @param streetSuffixCode Database column STREET_SUFFIX_CODE SqlType(VARCHAR), Length(15,true)
   *  @param localityPid Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true)
   *  @param gnafStreetPid Database column GNAF_STREET_PID SqlType(VARCHAR), Length(15,true)
   *  @param gnafStreetConfidence Database column GNAF_STREET_CONFIDENCE SqlType(INTEGER)
   *  @param gnafReliabilityCode Database column GNAF_RELIABILITY_CODE SqlType(INTEGER) */
  case class StreetLocalityRow(streetLocalityPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], streetClassCode: Char, streetName: String, streetTypeCode: Option[String], streetSuffixCode: Option[String], localityPid: String, gnafStreetPid: Option[String], gnafStreetConfidence: Option[Int], gnafReliabilityCode: Int)
  /** GetResult implicit for fetching StreetLocalityRow objects using plain SQL queries */
  implicit def GetResultStreetLocalityRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Char], e4: GR[Option[String]], e5: GR[Option[Int]], e6: GR[Int]): GR[StreetLocalityRow] = GR{
    prs => import prs._
    StreetLocalityRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[Char], <<[String], <<?[String], <<?[String], <<[String], <<?[String], <<?[Int], <<[Int]))
  }
  /** Table description of table STREET_LOCALITY. Objects of this class serve as prototypes for rows in queries. */
  class StreetLocality(_tableTag: Tag) extends Table[StreetLocalityRow](_tableTag, "STREET_LOCALITY") {
    def * = (streetLocalityPid, dateCreated, dateRetired, streetClassCode, streetName, streetTypeCode, streetSuffixCode, localityPid, gnafStreetPid, gnafStreetConfidence, gnafReliabilityCode) <> (StreetLocalityRow.tupled, StreetLocalityRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(streetLocalityPid), Rep.Some(dateCreated), dateRetired, Rep.Some(streetClassCode), Rep.Some(streetName), streetTypeCode, streetSuffixCode, Rep.Some(localityPid), gnafStreetPid, gnafStreetConfidence, Rep.Some(gnafReliabilityCode)).shaped.<>({r=>import r._; _1.map(_=> StreetLocalityRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6, _7, _8.get, _9, _10, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column STREET_LOCALITY_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val streetLocalityPid: Rep[String] = column[String]("STREET_LOCALITY_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column STREET_CLASS_CODE SqlType(CHAR) */
    val streetClassCode: Rep[Char] = column[Char]("STREET_CLASS_CODE")
    /** Database column STREET_NAME SqlType(VARCHAR), Length(100,true) */
    val streetName: Rep[String] = column[String]("STREET_NAME", O.Length(100,varying=true))
    /** Database column STREET_TYPE_CODE SqlType(VARCHAR), Length(15,true) */
    val streetTypeCode: Rep[Option[String]] = column[Option[String]]("STREET_TYPE_CODE", O.Length(15,varying=true))
    /** Database column STREET_SUFFIX_CODE SqlType(VARCHAR), Length(15,true) */
    val streetSuffixCode: Rep[Option[String]] = column[Option[String]]("STREET_SUFFIX_CODE", O.Length(15,varying=true))
    /** Database column LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val localityPid: Rep[String] = column[String]("LOCALITY_PID", O.Length(15,varying=true))
    /** Database column GNAF_STREET_PID SqlType(VARCHAR), Length(15,true) */
    val gnafStreetPid: Rep[Option[String]] = column[Option[String]]("GNAF_STREET_PID", O.Length(15,varying=true))
    /** Database column GNAF_STREET_CONFIDENCE SqlType(INTEGER) */
    val gnafStreetConfidence: Rep[Option[Int]] = column[Option[Int]]("GNAF_STREET_CONFIDENCE")
    /** Database column GNAF_RELIABILITY_CODE SqlType(INTEGER) */
    val gnafReliabilityCode: Rep[Int] = column[Int]("GNAF_RELIABILITY_CODE")

    /** Foreign key referencing GeocodeReliabilityAut (database name STREET_LOCALITY_FK1) */
    lazy val geocodeReliabilityAutFk = foreignKey("STREET_LOCALITY_FK1", gnafReliabilityCode, GeocodeReliabilityAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Locality (database name STREET_LOCALITY_FK2) */
    lazy val localityFk = foreignKey("STREET_LOCALITY_FK2", localityPid, Locality)(r => r.localityPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing StreetClassAut (database name STREET_LOCALITY_FK3) */
    lazy val streetClassAutFk = foreignKey("STREET_LOCALITY_FK3", streetClassCode, StreetClassAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing StreetSuffixAut (database name STREET_LOCALITY_FK4) */
    lazy val streetSuffixAutFk = foreignKey("STREET_LOCALITY_FK4", streetSuffixCode, StreetSuffixAut)(r => Rep.Some(r.code), onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing StreetTypeAut (database name STREET_LOCALITY_FK5) */
    lazy val streetTypeAutFk = foreignKey("STREET_LOCALITY_FK5", streetTypeCode, StreetTypeAut)(r => Rep.Some(r.code), onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)

    /** Index over (streetName) (database name STREET_LOCALITY_NAME_IDX) */
    val index1 = index("STREET_LOCALITY_NAME_IDX", streetName)
  }
  /** Collection-like TableQuery object for table StreetLocality */
  lazy val StreetLocality = new TableQuery(tag => new StreetLocality(tag))

  /** Entity class storing rows of table StreetLocalityAlias
   *  @param streetLocalityAliasPid Database column STREET_LOCALITY_ALIAS_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param streetLocalityPid Database column STREET_LOCALITY_PID SqlType(VARCHAR), Length(15,true)
   *  @param streetName Database column STREET_NAME SqlType(VARCHAR), Length(100,true)
   *  @param streetTypeCode Database column STREET_TYPE_CODE SqlType(VARCHAR), Length(15,true)
   *  @param streetSuffixCode Database column STREET_SUFFIX_CODE SqlType(VARCHAR), Length(15,true)
   *  @param aliasTypeCode Database column ALIAS_TYPE_CODE SqlType(VARCHAR), Length(10,true) */
  case class StreetLocalityAliasRow(streetLocalityAliasPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], streetLocalityPid: String, streetName: String, streetTypeCode: Option[String], streetSuffixCode: Option[String], aliasTypeCode: String)
  /** GetResult implicit for fetching StreetLocalityAliasRow objects using plain SQL queries */
  implicit def GetResultStreetLocalityAliasRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[String]]): GR[StreetLocalityAliasRow] = GR{
    prs => import prs._
    StreetLocalityAliasRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<[String], <<?[String], <<?[String], <<[String]))
  }
  /** Table description of table STREET_LOCALITY_ALIAS. Objects of this class serve as prototypes for rows in queries. */
  class StreetLocalityAlias(_tableTag: Tag) extends Table[StreetLocalityAliasRow](_tableTag, "STREET_LOCALITY_ALIAS") {
    def * = (streetLocalityAliasPid, dateCreated, dateRetired, streetLocalityPid, streetName, streetTypeCode, streetSuffixCode, aliasTypeCode) <> (StreetLocalityAliasRow.tupled, StreetLocalityAliasRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(streetLocalityAliasPid), Rep.Some(dateCreated), dateRetired, Rep.Some(streetLocalityPid), Rep.Some(streetName), streetTypeCode, streetSuffixCode, Rep.Some(aliasTypeCode)).shaped.<>({r=>import r._; _1.map(_=> StreetLocalityAliasRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6, _7, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column STREET_LOCALITY_ALIAS_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val streetLocalityAliasPid: Rep[String] = column[String]("STREET_LOCALITY_ALIAS_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column STREET_LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val streetLocalityPid: Rep[String] = column[String]("STREET_LOCALITY_PID", O.Length(15,varying=true))
    /** Database column STREET_NAME SqlType(VARCHAR), Length(100,true) */
    val streetName: Rep[String] = column[String]("STREET_NAME", O.Length(100,varying=true))
    /** Database column STREET_TYPE_CODE SqlType(VARCHAR), Length(15,true) */
    val streetTypeCode: Rep[Option[String]] = column[Option[String]]("STREET_TYPE_CODE", O.Length(15,varying=true))
    /** Database column STREET_SUFFIX_CODE SqlType(VARCHAR), Length(15,true) */
    val streetSuffixCode: Rep[Option[String]] = column[Option[String]]("STREET_SUFFIX_CODE", O.Length(15,varying=true))
    /** Database column ALIAS_TYPE_CODE SqlType(VARCHAR), Length(10,true) */
    val aliasTypeCode: Rep[String] = column[String]("ALIAS_TYPE_CODE", O.Length(10,varying=true))

    /** Foreign key referencing StreetLocality (database name STREET_LOCALITY_ALIAS_FK2) */
    lazy val streetLocalityFk = foreignKey("STREET_LOCALITY_ALIAS_FK2", streetLocalityPid, StreetLocality)(r => r.streetLocalityPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing StreetLocalityAliasTypeAut (database name STREET_LOCALITY_ALIAS_FK1) */
    lazy val streetLocalityAliasTypeAutFk = foreignKey("STREET_LOCALITY_ALIAS_FK1", aliasTypeCode, StreetLocalityAliasTypeAut)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing StreetSuffixAut (database name STREET_LOCALITY_ALIAS_FK3) */
    lazy val streetSuffixAutFk = foreignKey("STREET_LOCALITY_ALIAS_FK3", streetSuffixCode, StreetSuffixAut)(r => Rep.Some(r.code), onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing StreetTypeAut (database name STREET_LOCALITY_ALIAS_FK4) */
    lazy val streetTypeAutFk = foreignKey("STREET_LOCALITY_ALIAS_FK4", streetTypeCode, StreetTypeAut)(r => Rep.Some(r.code), onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table StreetLocalityAlias */
  lazy val StreetLocalityAlias = new TableQuery(tag => new StreetLocalityAlias(tag))

  /** Entity class storing rows of table StreetLocalityAliasTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(10,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(15,true) */
  case class StreetLocalityAliasTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching StreetLocalityAliasTypeAutRow objects using plain SQL queries */
  implicit def GetResultStreetLocalityAliasTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[StreetLocalityAliasTypeAutRow] = GR{
    prs => import prs._
    StreetLocalityAliasTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table STREET_LOCALITY_ALIAS_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class StreetLocalityAliasTypeAut(_tableTag: Tag) extends Table[StreetLocalityAliasTypeAutRow](_tableTag, "STREET_LOCALITY_ALIAS_TYPE_AUT") {
    def * = (code, name, description) <> (StreetLocalityAliasTypeAutRow.tupled, StreetLocalityAliasTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> StreetLocalityAliasTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(10,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(10,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(15,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(15,varying=true))
  }
  /** Collection-like TableQuery object for table StreetLocalityAliasTypeAut */
  lazy val StreetLocalityAliasTypeAut = new TableQuery(tag => new StreetLocalityAliasTypeAut(tag))

  /** Entity class storing rows of table StreetLocalityPoint
   *  @param streetLocalityPointPid Database column STREET_LOCALITY_POINT_PID SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param dateCreated Database column DATE_CREATED SqlType(DATE)
   *  @param dateRetired Database column DATE_RETIRED SqlType(DATE)
   *  @param streetLocalityPid Database column STREET_LOCALITY_PID SqlType(VARCHAR), Length(15,true)
   *  @param boundaryExtent Database column BOUNDARY_EXTENT SqlType(INTEGER)
   *  @param planimetricAccuracy Database column PLANIMETRIC_ACCURACY SqlType(DECIMAL)
   *  @param longitude Database column LONGITUDE SqlType(DECIMAL)
   *  @param latitude Database column LATITUDE SqlType(DECIMAL) */
  case class StreetLocalityPointRow(streetLocalityPointPid: String, dateCreated: java.sql.Date, dateRetired: Option[java.sql.Date], streetLocalityPid: String, boundaryExtent: Option[Int], planimetricAccuracy: Option[scala.math.BigDecimal], longitude: Option[scala.math.BigDecimal], latitude: Option[scala.math.BigDecimal])
  /** GetResult implicit for fetching StreetLocalityPointRow objects using plain SQL queries */
  implicit def GetResultStreetLocalityPointRow(implicit e0: GR[String], e1: GR[java.sql.Date], e2: GR[Option[java.sql.Date]], e3: GR[Option[Int]], e4: GR[Option[scala.math.BigDecimal]]): GR[StreetLocalityPointRow] = GR{
    prs => import prs._
    StreetLocalityPointRow.tupled((<<[String], <<[java.sql.Date], <<?[java.sql.Date], <<[String], <<?[Int], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }
  /** Table description of table STREET_LOCALITY_POINT. Objects of this class serve as prototypes for rows in queries. */
  class StreetLocalityPoint(_tableTag: Tag) extends Table[StreetLocalityPointRow](_tableTag, "STREET_LOCALITY_POINT") {
    def * = (streetLocalityPointPid, dateCreated, dateRetired, streetLocalityPid, boundaryExtent, planimetricAccuracy, longitude, latitude) <> (StreetLocalityPointRow.tupled, StreetLocalityPointRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(streetLocalityPointPid), Rep.Some(dateCreated), dateRetired, Rep.Some(streetLocalityPid), boundaryExtent, planimetricAccuracy, longitude, latitude).shaped.<>({r=>import r._; _1.map(_=> StreetLocalityPointRow.tupled((_1.get, _2.get, _3, _4.get, _5, _6, _7, _8)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column STREET_LOCALITY_POINT_PID SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val streetLocalityPointPid: Rep[String] = column[String]("STREET_LOCALITY_POINT_PID", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column DATE_CREATED SqlType(DATE) */
    val dateCreated: Rep[java.sql.Date] = column[java.sql.Date]("DATE_CREATED")
    /** Database column DATE_RETIRED SqlType(DATE) */
    val dateRetired: Rep[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_RETIRED")
    /** Database column STREET_LOCALITY_PID SqlType(VARCHAR), Length(15,true) */
    val streetLocalityPid: Rep[String] = column[String]("STREET_LOCALITY_PID", O.Length(15,varying=true))
    /** Database column BOUNDARY_EXTENT SqlType(INTEGER) */
    val boundaryExtent: Rep[Option[Int]] = column[Option[Int]]("BOUNDARY_EXTENT")
    /** Database column PLANIMETRIC_ACCURACY SqlType(DECIMAL) */
    val planimetricAccuracy: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("PLANIMETRIC_ACCURACY")
    /** Database column LONGITUDE SqlType(DECIMAL) */
    val longitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LONGITUDE")
    /** Database column LATITUDE SqlType(DECIMAL) */
    val latitude: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("LATITUDE")

    /** Foreign key referencing StreetLocality (database name STREET_LOCALITY_POINT_FK1) */
    lazy val streetLocalityFk = foreignKey("STREET_LOCALITY_POINT_FK1", streetLocalityPid, StreetLocality)(r => r.streetLocalityPid, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table StreetLocalityPoint */
  lazy val StreetLocalityPoint = new TableQuery(tag => new StreetLocalityPoint(tag))

  /** Entity class storing rows of table StreetSuffixAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
  case class StreetSuffixAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching StreetSuffixAutRow objects using plain SQL queries */
  implicit def GetResultStreetSuffixAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[StreetSuffixAutRow] = GR{
    prs => import prs._
    StreetSuffixAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table STREET_SUFFIX_AUT. Objects of this class serve as prototypes for rows in queries. */
  class StreetSuffixAut(_tableTag: Tag) extends Table[StreetSuffixAutRow](_tableTag, "STREET_SUFFIX_AUT") {
    def * = (code, name, description) <> (StreetSuffixAutRow.tupled, StreetSuffixAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> StreetSuffixAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(30,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(30,varying=true))
  }
  /** Collection-like TableQuery object for table StreetSuffixAut */
  lazy val StreetSuffixAut = new TableQuery(tag => new StreetSuffixAut(tag))

  /** Entity class storing rows of table StreetTypeAut
   *  @param code Database column CODE SqlType(VARCHAR), PrimaryKey, Length(15,true)
   *  @param name Database column NAME SqlType(VARCHAR), Length(50,true)
   *  @param description Database column DESCRIPTION SqlType(VARCHAR), Length(15,true) */
  case class StreetTypeAutRow(code: String, name: String, description: Option[String])
  /** GetResult implicit for fetching StreetTypeAutRow objects using plain SQL queries */
  implicit def GetResultStreetTypeAutRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[StreetTypeAutRow] = GR{
    prs => import prs._
    StreetTypeAutRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table STREET_TYPE_AUT. Objects of this class serve as prototypes for rows in queries. */
  class StreetTypeAut(_tableTag: Tag) extends Table[StreetTypeAutRow](_tableTag, "STREET_TYPE_AUT") {
    def * = (code, name, description) <> (StreetTypeAutRow.tupled, StreetTypeAutRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(name), description).shaped.<>({r=>import r._; _1.map(_=> StreetTypeAutRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE SqlType(VARCHAR), PrimaryKey, Length(15,true) */
    val code: Rep[String] = column[String]("CODE", O.PrimaryKey, O.Length(15,varying=true))
    /** Database column NAME SqlType(VARCHAR), Length(50,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column DESCRIPTION SqlType(VARCHAR), Length(15,true) */
    val description: Rep[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(15,varying=true))
  }
  /** Collection-like TableQuery object for table StreetTypeAut */
  lazy val StreetTypeAut = new TableQuery(tag => new StreetTypeAut(tag))
}
