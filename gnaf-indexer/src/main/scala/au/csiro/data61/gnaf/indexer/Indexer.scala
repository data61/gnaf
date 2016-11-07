package au.csiro.data61.gnaf.indexer

import java.io.File

import scala.io.Source

import org.apache.lucene.document.{ Document, DoublePoint, Field }

import au.csiro.data61.gnaf.lucene.GnafLucene._
import au.csiro.data61.gnaf.lucene.LuceneUtil.directory
import au.csiro.data61.gnaf.util.Gnaf.Address
import au.csiro.data61.gnaf.util.Gnaf.JsonProtocol.addressFormat
import au.csiro.data61.gnaf.util.Util.getLogger
import resource.managed
import spray.json.pimpString


object Indexer {
  val log = getLogger(getClass)
  
  case class CliOption(indexDir: File)
  val defaultCliOption = CliOption(new File("./indexDir"))
    
  def main(args: Array[String]) = {
    val parser = new scopt.OptionParser[CliOption]("gnaf-indexer") {
      head("gnaf-lucene-indexer", "0.x")
      note("Load GNAF JSON into a Lucene index")
      opt[File]('i', "indexDir") action { (x, c) =>
        c.copy(indexDir = x)
      } text (s"Lucene index directory, default ${defaultCliOption.indexDir}")
      help("help") text ("prints this usage text")
    }
    parser.parse(args, defaultCliOption) foreach run
    log.info("done")
  }

  def addrToDoc(line: String) = {
    val addr = line.parseJson.convertTo[Address]
    val (d61Address, noneCount, d61AddressNoAlias) = addr.toD61Address
    val doc = new Document
    doc.add(new Field(F_JSON, line, storedNotIndexedFieldType))
    for (l <- addr.location) doc.add(new DoublePoint(F_LOCATION, l.lat.toDouble, l.lon.toDouble))
    for (a <- d61Address) doc.add(new Field(F_ADDRESS, a, addressFieldType))
    for {
      f <- addr.flat.toOptStr if addr.level.toOptStr.isEmpty
      n <- addr.numberFirst.toOptStr
    } doc.add(new Field(F_ADDRESS, f + BIGRAM_SEPARATOR + n, addressFieldType)) // explicitly add flat + street num bigram without extra unigrams 
    for (i <- 0 until noneCount) doc.add(new Field(F_MISSING_DATA, MISSING_DATA_TOKEN, missingDataFieldType))
    doc.add(new Field(F_ADDRESS_NOALIAS, d61AddressNoAlias, storedNotIndexedFieldType))
    
    doc
  }
  
  def run(c: CliOption) = {
    for {
      indexer <- managed(mkIndexer(directory(c.indexDir)))
      line <- Source.fromInputStream(System.in, "UTF-8").getLines
    } {
      indexer.addDocument(addrToDoc(line))
    }
  }
}