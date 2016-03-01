package au.com.data61.gnaf.indexer

import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import resource.managed
import au.com.data61.gnaf.db.Tables._
import scala.concurrent.Await

object Main {
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

  def run(c: Config) = {
    for (db <- managed(Database.forConfig("gnafDb"))) {
      // is this really better than SQL?
      // the auto-generated mapping has translated `number_first numeric(6)` to scala.math.BigDecimal whereas Int would has done fine!
      // likewise for most numeric columns, but BigDecimal is probably appropriate for geo-code stuff latitude, longitude, planimetric_accuracy
      // i think this choice is made by H2
      // TODO: maybe we should change the table creation SQL to use the H2 type INT, INTEGER, INT4 for numeric(x) x <= 9.
      val q = for {
        (ad, lta) <- AddressDetail joinLeft LevelTypeAut on (_.levelTypeCode === _.code) if (ad.numberFirst === BigDecimal(14) && ad.confidence > BigDecimal(-1))
        sl <- ad.streetLocalityFk if sl.streetName === "TYTHERLEIGH"
        loc <- sl.localityFk
        st <- loc.stateFk
      } yield (
          ad.flatTypeCode, ad.flatNumberPrefix, ad.flatNumber, ad.flatNumberSuffix,
          lta.map(_.name), ad.levelNumberPrefix, ad.levelNumber, ad.levelNumberSuffix, 
          ad.numberFirstPrefix, ad.numberFirst, ad.numberFirstSuffix,
          ad.numberLastPrefix, ad.numberLast, ad.numberLastSuffix,
          sl.streetName, sl.streetTypeCode,
          loc.localityName, st.stateAbbreviation, ad.postcode)
      val f = db.run(q.result)
      f.foreach(_.foreach(println))
      Await.result(f, 30.seconds)
    }
  }
}