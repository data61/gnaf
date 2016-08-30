package au.csiro.data61.gnaf.lucene.util

import org.scalatest.{ FlatSpec, Matchers }

import GnafLucene.countOccurrences

class GnafLuceneTest extends FlatSpec with Matchers {
  
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
  
}