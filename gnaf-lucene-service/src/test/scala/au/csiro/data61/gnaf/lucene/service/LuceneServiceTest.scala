package au.csiro.data61.gnaf.lucene.service

import org.apache.lucene.document.Document
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.store.{ Directory, RAMDirectory }
import org.scalatest.{ Finders, FlatSpec, Matchers }

import LuceneService.{ QueryParam, mkQuery, toHit, toResult, toSort }
import au.csiro.data61.gnaf.lucene.service.LuceneService.Hit
import au.csiro.data61.gnaf.lucene.util.GnafLucene.{ AddressSimilarity, F_D61ADDRESS, F_D61ADDRESS_NOALIAS, analyzer, indexingFieldTypes }
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.Indexing.{ Indexer, docAdder }
import au.csiro.data61.gnaf.util.Util.getLogger
import resource.managed
import au.csiro.data61.gnaf.lucene.util.LuceneUtil.Searching.Searcher

/**
 * More a test bed for examining unexpected results than a conventional unit test.
 */
class LuceneServiceTest extends FlatSpec with Matchers {
  val log = getLogger(getClass)
  
  def mkIndexer(dir: Directory) = new Indexer(null, true, analyzer) { 
    override protected def directory = dir
    override protected def indexWriterConfig = {
      val c = super.indexWriterConfig
      c.setSimilarity(new AddressSimilarity)
      c
    }
  }
  
  case class Hit2(id: Int, score: Float, d61Address: List[String], d61AddressNoAlias: String)
  def toHit2(scoreDoc: ScoreDoc, doc: Document) = {
    Hit2(scoreDoc.doc, scoreDoc.score, doc.getValues(F_D61ADDRESS).toList, doc.get(F_D61ADDRESS_NOALIAS))
  }
  
  case class Result2(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit2], error: Option[String])
  def toResult2(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit2], error: Option[String])
    = Result2(totalHits, elapsedSecs, hits, error)
  
  def mkSearcher(dir: Directory) = {
    val s = new Searcher(null, toHit2, toResult2, toSort) { 
      override protected def directory = dir 
    }
    s.searcher.setSimilarity(new AddressSimilarity)
    s
  }
  
  "searcher" should "find" in {
    val dir = new RAMDirectory
    for {
      indexer <- managed(mkIndexer(dir))
      (d61Address, d61AddressNoAlias) <- Seq(
          (Seq("55 UPPER PAPER MILLS ROAD", "FYANSFORD VIC 3218"), "55 UPPER PAPER MILLS ROAD FYANSFORD VIC 321"),
          (Seq("45 UPPER PAPER MILLS ROAD", "FYANSFORD VIC 3218"), "45 UPPER PAPER MILLS ROAD FYANSFORD VIC 321"),
          (Seq("155-205 UPPER PAPER MILLS ROAD", "FYANSFORD VIC 3218"), "155-205 UPPER PAPER MILLS ROAD FYANSFORD VIC 321")
        )
    } {
      val (doc, add) = docAdder(indexingFieldTypes)
      d61Address foreach { a => add(F_D61ADDRESS, a, false) }
      add(F_D61ADDRESS_NOALIAS, d61AddressNoAlias, false)
      indexer.add(doc)
    }
    
    for (searcher <- managed(mkSearcher(dir))) {
      // addr: String, numHits: Int, minFuzzyLength: Int, fuzzyMaxEdits: Int, fuzzyPrefixLength: Int
      val q = mkQuery(QueryParam("155-205 UPPER PAPER MILLS ROAD FYANSFORD VIC 321", 3, 5, 0, 2))
      val r = searcher.search(q, None, 3, 0)
      for (h <- r.hits) {
        log.debug(h.toString)
        log.debug(searcher.searcher.explain(q, h.id).toString)
      }
      r.hits(0).d61AddressNoAlias should be("155-205 UPPER PAPER MILLS ROAD FYANSFORD VIC 321")
    }
    
  }
}