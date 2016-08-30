package au.csiro.data61.gnaf.lucene.util

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.LowerCaseFilter
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.shingle.ShingleFilter
import org.apache.lucene.index.FieldInvertState
import org.apache.lucene.search.similarities.ClassicSimilarity

import LuceneUtil.Indexing.mkFieldType


/**
 * GNAF specific field names, analyzers and scoring for Lucene.
 */
object GnafLucene {
  
  val F_JSON = "json"
  val F_D61ADDRESS = "d61Address"
  val F_D61ADDRESS_NOALIAS = "d61AddressNoAlias"
  
  val whiteLowerAnalyzer = new Analyzer {
    
    override protected def createComponents(fieldName: String) = {
      val source = new WhitespaceTokenizer()
      val result = new LowerCaseFilter(source)
      new TokenStreamComponents(source, result)
    }
    
    override protected def getPositionIncrementGap(fieldName: String): Int = 100
  }
  
  val shingleWhiteLowerAnalyzer = new Analyzer {
    
    override protected def createComponents(fieldName: String) = {
      val source = new WhitespaceTokenizer()
      // ShingleFilter defaults are:
      //   minShingleSize = 2 (error if set < 2), maxShingleSize = 2
      //   outputUnigrams = true
      val result = new ShingleFilter(new LowerCaseFilter(source), 2, 2)
      new TokenStreamComponents(source, result)
    }
    
    override def getPositionIncrementGap(fieldName: String): Int = 100 // stop shingles matching across boundaries
  }
  
  /** count occurrences of x in s, x must be non-empty */
  def countOccurrences(s: String, x: String) = {
    assert(x.nonEmpty)
    var n = 0
    var i = 0
    while (i < s.length - x.length) {
      i = s.indexOf(x, i)
      if (i == -1) i = s.length
      else {
        n += 1
        i += x.length
      }
    }
    n
  }
  
  /** get n-gram size n */
  def shingleSize(s: String) = countOccurrences(s, ShingleFilter.DEFAULT_TOKEN_SEPARATOR) + 1
  
  class AddressSimilarity extends ClassicSimilarity {
    override def lengthNorm(state: FieldInvertState) = state.getBoost // no length norm, don't penalize multiple aliases
    override def tf(freq: Float): Float = 1.0f // don't boost street and locality name being the same
    override def idf(docFreq: Long, docCount: Long): Float = 1.0f // don't penalize SMITH STREET for being common
    // coord factor boosts docs that match more query terms, so correct match scores higher than spuriously same street and locality name
  }
  
  // shouldn't be essential as "d61Address" is the only tokenized (analyzed) field
  // val analyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer, Map(F_D61ADDRESS -> shingleWhiteLowerAnalyzer))
  val analyzer = shingleWhiteLowerAnalyzer
    
  val indexingFieldTypes = Map(
    // defaults are:
    //   tokenized: Boolean = false, stored: Boolean = true, indexed: Boolean = true,
    //   termVectors: Boolean = false, opt: IndexOptions = DOCS_AND_FREQS
    F_JSON -> mkFieldType(indexed = false),
    F_D61ADDRESS -> mkFieldType(tokenized = true),
    F_D61ADDRESS_NOALIAS -> mkFieldType(indexed = false)
    )
    
}