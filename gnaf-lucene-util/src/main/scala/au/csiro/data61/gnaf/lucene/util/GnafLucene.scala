package au.csiro.data61.gnaf.lucene.util

import scala.collection.JavaConversions.mapAsJavaMap

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.core.{ KeywordAnalyzer, LowerCaseFilter, WhitespaceTokenizer }
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.shingle.ShingleFilter
import org.apache.lucene.index.IndexOptions

import LuceneUtil.Indexing.mkFieldType


/**
 * GNAF specific field names and analyzers for Lucene.
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
  }
    
  // shouldn't be essential as "d61Address" is the only tokenized (analyzed) field
  // val analyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer, Map(F_D61ADDRESS -> shingleWhiteLowerAnalyzer))
  val analyzer = shingleWhiteLowerAnalyzer
    
  val indexingFieldTypes = Map(
    // defaults are:
    //   tokenized: Boolean = false, stored: Boolean = true, indexed: Boolean = true,
    //   termVectors: Boolean = false, opt: IndexOptions = DOCS_AND_FREQS
    F_JSON -> mkFieldType(indexed = false),
    F_D61ADDRESS -> mkFieldType(tokenized = true, opt = IndexOptions.DOCS),
    F_D61ADDRESS_NOALIAS -> mkFieldType(indexed = false)
    )
    
}