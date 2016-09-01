package au.csiro.data61.gnaf.lucene

import org.apache.lucene.document.{ Document, Field }
import org.apache.lucene.search.{ MatchAllDocsQuery, ScoreDoc }
import org.apache.lucene.store.{ Directory, RAMDirectory }
import org.scalatest.{ FlatSpec, Matchers }

import GnafLucene._
import LuceneUtil._
import au.csiro.data61.gnaf.util.Util.getLogger
import resource.managed

import java.io.File

import org.apache.lucene.document.DoublePoint

/**
 * More a test bed for examining unexpected results than a conventional unit test.
 */
class GnafLuceneTest extends FlatSpec with Matchers {
  val log = getLogger(getClass)
  
  val s = "some test string"

  "countOccurences" should "count" in {
    for {
      (x, n) <- Seq((" ", 2), ("in", 1), (",", 0))
    } countOccurrences(s, x) should be(n)
    
    countOccurrences("", "some") should be(0)
  }
  
  it should "throw AssertionError on empty find string" in {
    a [AssertionError] should be thrownBy {
      countOccurrences(s, "")
    } 
  }
  
  case class Hit(id: Int, score: Float, d61Address: List[String], d61AddressNoAlias: String)
  def toHit(scoreDoc: ScoreDoc, doc: Document) = {
    Hit(scoreDoc.doc, scoreDoc.score, doc.getValues(F_D61ADDRESS).toList, doc.get(F_D61ADDRESS_NOALIAS))
  }
  
  case class Result(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
  def toResult(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
    = Result(totalHits, elapsedSecs, hits, error)
  
  def mkSearcher(dir: Directory) = {
    val s = new Searcher(dir, toHit, toResult)
    s.searcher.setSimilarity(AddressSimilarity)
    s
  }
  
  def mkDoc(addr: (Seq[String], String, Double, Double)) = {
    val d = new Document
    // d.add(new Field(F_JSON, "", storedNotIndexedFieldType))
    d.add(new DoublePoint(F_LOCATION, addr._3, addr._4))
    for (a <- addr._1) { 
      log.debug(s"mkDoc: add: $a")
      d.add(new Field(F_D61ADDRESS, a, d61AddrFieldType))
    }
    d.add(new Field(F_D61ADDRESS_NOALIAS, addr._2, storedNotIndexedFieldType))
    d
  }
  
  "searcher" should "find" in {
	  for (dir <- managed(new RAMDirectory)) {
		  for (indexer <- managed(mkIndexer(dir))) {
			  Seq(
				  (Seq("55 UPPER PAPER MILLS ROAD", "FYANSFORD VIC 3218"), "55 UPPER PAPER MILLS ROAD FYANSFORD VIC 321", 1.5d, 11.5d),
				  (Seq("45 UPPER PAPER MILLS ROAD", "FYANSFORD VIC 3218"), "45 UPPER PAPER MILLS ROAD FYANSFORD VIC 321", 0.5d, 10.5d),
				  (Seq("155-205 UPPER PAPER MILLS ROAD", "FYANSFORD VIC 3218"), "155-205 UPPER PAPER MILLS ROAD FYANSFORD VIC 321", 0d, 10d)
			  ).foreach(a => indexer.addDocument(mkDoc(a)))
		  } // indexer.close

		  for (searcher <- managed(mkSearcher(dir))) {
			  // addr: String, numHits: Int, minFuzzyLength: Int, fuzzyMaxEdits: Int, fuzzyPrefixLength: Int
			  val q = QueryParam("155-205 UPPER PAPER MILLS ROAD FYANSFORD VIC 321", 3, None, None).toQuery
			  val r = searcher.search(q, 3)
			  log.debug(r.toString)
			  for (h <- r.hits) {
				  log.debug(h.toString)
				  log.debug(searcher.searcher.explain(q, h.id).toString)
			  }
	  	  r.hits(0).d61AddressNoAlias should be("155-205 UPPER PAPER MILLS ROAD FYANSFORD VIC 321")
	  	  
	  	  val gq = DoublePoint.newRangeQuery(F_LOCATION, Array[Double](-0.25, 9.75), Array[Double](0.75, 10.75))
	  	  val gr = searcher.search(gq, 3)
			  log.debug(gr.toString)
			  gr.hits.size should be(2)
	  	  val expected = Set("155-205 UPPER PAPER MILLS ROAD FYANSFORD VIC 321", "45 UPPER PAPER MILLS ROAD FYANSFORD VIC 321")
	  	  gr.hits.foreach(h => expected.contains(h.d61AddressNoAlias) should be(true))
		  } // searcher.close
	  } // dir.close
  }
}