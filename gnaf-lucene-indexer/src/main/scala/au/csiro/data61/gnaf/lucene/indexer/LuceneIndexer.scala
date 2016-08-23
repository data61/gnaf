package au.csiro.data61.gnaf.lucene.indexer

import java.io.File

import scala.io.Source

import au.csiro.data61.gnaf.lucene.util.GnafLucene.{ F_D61ADDRESS, F_D61ADDRESS_NOALIAS, F_JSON, analyzer, indexingFieldTypes }
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.Indexing.{ Indexer, docAdder }
import au.csiro.data61.gnaf.util.Gnaf.{ Address, D61Address }
import au.csiro.data61.gnaf.util.Gnaf.JsonProtocol.addressFormat
import au.csiro.data61.gnaf.util.Util.getLogger
import resource.managed
import spray.json.pimpString


object LuceneIndexer {
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

  def run(c: CliOption) = {
    for {
      indexer <- managed(new Indexer(c.indexDir, true, analyzer))
      line <- Source.fromInputStream(System.in, "UTF-8").getLines
    } {
      val addr = line.parseJson.convertTo[Address]
      val D61Address(d61Address, d61AddressNoAlias) = addr.toD61Address
      val (doc, add) = docAdder(indexingFieldTypes)
      add(F_JSON, line, false)
      d61Address foreach { a => add(F_D61ADDRESS, a, false) }
      add(F_D61ADDRESS_NOALIAS, d61AddressNoAlias, false)
      indexer.add(doc)
    }
  }
}