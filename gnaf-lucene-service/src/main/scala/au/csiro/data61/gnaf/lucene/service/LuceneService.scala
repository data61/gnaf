package au.csiro.data61.gnaf.lucene.service

import java.io.File

import scala.io.Source

import au.csiro.data61.gnaf.lucene.util.GnafLucene.{ F_D61ADDRESS, F_D61ADDRESS_NOALIAS, F_JSON, analyzer, indexingFieldTypes }
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.Indexing.{ Indexer, docAdder }
import au.csiro.data61.gnaf.util.Gnaf.{ Address, D61Address }
import au.csiro.data61.gnaf.util.Gnaf.JsonProtocol.addressFormat
import au.csiro.data61.gnaf.util.Util.getLogger
import resource.managed
import spray.json.pimpString
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.tokenIter
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.Searching.Searcher
import org.apache.lucene.search.Sort
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BooleanClause
import BooleanClause.Occur.SHOULD
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term
import org.apache.lucene.document.Document
import org.apache.lucene.search.similarities.TFIDFSimilarity
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.FuzzyQuery

object LuceneService {
  val log = getLogger(getClass)
  
  case class CliOption(indexDir: File, numHits: Int, minFuzzyLength: Int, fuzzyMaxEdits: Int, fuzzyPrefixLength: Int)
  val defaultCliOption = CliOption(new File("./indexDir"), 1, 5, 2, 2)

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[CliOption]("gnaf-indexer") {
      head("gnaf-lucene-service", "0.x")
      note("Perform searches on GNAF Lucene index")
      opt[File]('i', "indexDir") action { (x, c) =>
        c.copy(indexDir = x)
      } text (s"Lucene index directory, default ${defaultCliOption.indexDir}")
      opt[Int]('h', "numHits") action { (x, c) =>
        c.copy(numHits = x)
      } text (s"number of search hits to retrieve, default ${defaultCliOption.numHits}")
      opt[Int]('f', "minFuzzyLength") action { (x, c) =>
        c.copy(minFuzzyLength = x)
      } text (s"min query term length for fuzzy match, default ${defaultCliOption.minFuzzyLength}")
      opt[Int]('e', "fuzzyMaxEdits") action { (x, c) =>
        c.copy(fuzzyMaxEdits = x)
      } text (s"max edits for a fuzzy match, default ${defaultCliOption.fuzzyMaxEdits}")
      opt[Int]('p', "fuzzyPrefixLength") action { (x, c) =>
        c.copy(fuzzyPrefixLength = x)
      } text (s"initial chars that must match exactly for a fuzzy match, default ${defaultCliOption.fuzzyPrefixLength}")
      help("help") text ("prints this usage text")
    }
    parser.parse(args, defaultCliOption) foreach run
    log.info("done")
  }
  
  val fieldToLoad = Set(F_JSON, F_D61ADDRESS, F_D61ADDRESS_NOALIAS)
  
  case class Hit(score: Float, json: String, d61Address: List[String], d61AddressNoAlias: String)
  def toHit(score: Float, doc: Document) = {
    Hit(score, doc.get(F_JSON), doc.getValues(F_D61ADDRESS).toList, doc.get(F_D61ADDRESS_NOALIAS))
  }
  
  case class Result(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
  def toResult(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
    = Result(totalHits, elapsedSecs, hits, error)
  
  def toSort(f: Option[String], asc: Boolean): Option[Sort] = None
  
  def query(c: CliOption, addr: String) = {
    tokenIter(analyzer, F_D61ADDRESS, addr).foldLeft(new BooleanQuery.Builder){ (b, t) =>
      val term = new Term(F_D61ADDRESS, t)
      val query = if (t.length < c.minFuzzyLength) new TermQuery(term) else new FuzzyQuery(term, c.fuzzyMaxEdits, c.fuzzyPrefixLength)
      b.add(new BooleanClause(query, SHOULD))
      b
    }.build
  }
  
  class AddressSimilarity extends ClassicSimilarity {
    override def tf(freq: Float): Float = 1.0f
    override def idf(docFreq: Long, docCount: Long): Float = 1.0f
  }
  
  // Input generated with:
  // jq '.[]|.query, .queryPostcodeBeforeState, .queryTypo' ../gnaf-test/address-*.json | sed 's/"//g' > addresses.txt
  
  def run(c: CliOption) = {
    for {
      searcher <- managed(new Searcher(c.indexDir, toHit, toResult, toSort))
      _ = searcher.searcher.setSimilarity(new AddressSimilarity)
      addr <- Source.fromInputStream(System.in, "UTF-8").getLines
    } {
      val rslt = searcher.search(query(c, addr), None, c.numHits, 0)
      log.debug(s"rslt = $rslt")
    }
  }
}