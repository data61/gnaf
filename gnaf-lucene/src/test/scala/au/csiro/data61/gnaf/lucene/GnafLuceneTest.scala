package au.csiro.data61.gnaf.lucene

import org.apache.lucene.document.{ Document, DoublePoint, Field }
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.store.{ Directory, RAMDirectory }
import org.scalatest.{ Finders, FlatSpec, Matchers }

import GnafLucene._
import LuceneUtil.Searcher
import au.csiro.data61.gnaf.util.Util.getLogger
import resource.managed

/**
 * More a test bed for:<ul>
 * <li>examining unexpected results
 * <li>experimenting with search techniques
 * </ul>
 * than a conventional unit test.
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
    Hit(scoreDoc.doc, scoreDoc.score, doc.getValues(F_ADDRESS).toList, doc.get(F_ADDRESS_NOALIAS))
  }
  
  case class Result(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
  def toResult(totalHits: Int, elapsedSecs: Float, hits: Seq[Hit], error: Option[String])
    = Result(totalHits, elapsedSecs, hits, error)
  
  def mkSearcher(dir: Directory) = {
    val s = new Searcher(dir, toHit, toResult)
    s.searcher.setSimilarity(GnafSimilarity)
    s
  }
  
  def mkDoc(addr: (Seq[String], Option[String], Int, String, Double, Double)) = {
    val d = new Document
    for (a <- addr._1) { 
      log.debug(s"mkDoc: add: $a")
      d.add(new Field(F_ADDRESS, a, addressFieldType))
    }
    for (a <- addr._2) { 
      log.debug(s"mkDoc: add: $a")
      d.add(new Field(F_ADDRESS, a, addressFieldType))
    }
    for (i <- 0 until addr._3) d.add(new Field(F_MISSING_DATA, MISSING_DATA_TOKEN, missingDataFieldType))
    d.add(new Field(F_ADDRESS_NOALIAS, addr._4, storedNotIndexedFieldType))
    d.add(new DoublePoint(F_LOCATION, addr._5, addr._6))
    d
  }
  
  "searcher" should "find" in {
	  for (dir <- managed(new RAMDirectory)) {
		  for (indexer <- managed(mkIndexer(dir))) {
			  Seq( //                                                  v noneCount = number of fields with missing data: streetNo, build/site, flat, level
				  (Seq("3204 INVERNESS ROAD", "DUMGREE QLD 4715"), None, 3, "3204 INVERNESS ROAD DUMGREE QLD 4715", 0.5d, 10.5d),
				  (Seq("INVERNESS ROAD", "DUMGREE QLD 4715"),      None, 4, "INVERNESS ROAD DUMGREE QLD 4715", 0.7d, 11.5d),
				  (Seq("FLAT 1", "2400 INVERNESS ROAD", "DUMGREE QLD 4715"), Some("1" + BIGRAM_SEPARATOR + "2400"), 2, "FLAT 1 2400 INVERNESS ROAD DUMGREE QLD 4715", 0d, 10d)
			  ).foreach(a => indexer.addDocument(mkDoc(a)))
		  } // indexer.close

		  for (searcher <- managed(mkSearcher(dir))) {
			  // addr: String, numHits: Int, minFuzzyLength: Int, fuzzyMaxEdits: Int, fuzzyPrefixLength: Int
		    {
  			  val q = QueryParam("INVERNESS ROAD DUMGREE QLD 4715", 3, None, None).toQuery
  			  val r = searcher.search(q, 3)
  			  log.debug(r.toString)
  			  for (h <- r.hits) {
  				  log.debug(h.toString)
  				  log.debug(searcher.searcher.explain(q, h.id).toString)
  			  }
  	  	  r.hits(0).d61AddressNoAlias should be("INVERNESS ROAD DUMGREE QLD 4715")
  	  	  // Lucene docId is 0, 1, 2 in order that docs are indexed
  	  	  r.hits.map(_.id) should be(Seq(1, 0, 2)) // in order of decreasing noneCount: 4, 3, 2
		    }
	  	  
		    {
  	  	  val q = QueryParam("FLAT 1", 0, None, None).toQuery
  	  	  val r = searcher.search(q, 1)
  			  log.debug(r.toString)
  	  	  val h = r.hits(0)
  	  	  log.debug(h.toString)
  	  	  log.debug(searcher.searcher.explain(q, h.id).toString)
  	  	  
  	  	  val q2 = QueryParam("1 2400", 0, None, None).toQuery
  	  	  val r2 = searcher.search(q2, 1)
  			  log.debug(r2.toString)
  	  	  val h2 = r2.hits(0)
  	  	  log.debug(h2.toString)
  	  	  log.debug(searcher.searcher.explain(q2, h2.id).toString)
		    }
		    
		    {
	  	  val q = DoublePoint.newRangeQuery(F_LOCATION, Array[Double](-0.25, 9.75), Array[Double](0.75, 10.75))
	  	  val r = searcher.search(q, 3)
			  log.debug(r.toString)
			  r.hits.map(_.id).toSet should be(Set(0, 2)) // doc 1 not in box
		    }
		  } // searcher.close
	  } // dir.close
  }
}