package au.csiro.data61.gnaf.indexer

import java.io.File

import scala.io.Source

import org.apache.lucene.document.{ Document, DoublePoint, Field }
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.StringField

import au.csiro.data61.gnaf.lucene.GnafLucene.{ D61_NO_DATA, F_D61ADDRESS, F_D61ADDRESS_NOALIAS, F_D61NO_DATA, F_JSON, F_LOCATION, d61AddrFieldType, mkIndexer, storedNotIndexedFieldType }
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
    for (a <- d61Address) doc.add(new Field(F_D61ADDRESS, a, d61AddrFieldType))
    for (i <- 0 until noneCount) doc.add(new StringField(F_D61NO_DATA, D61_NO_DATA, Store.NO))
    doc.add(new Field(F_D61ADDRESS_NOALIAS, d61AddressNoAlias, storedNotIndexedFieldType))
    
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