package au.csiro.data61.gnaf.lucene

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.LowerCaseFilter
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.analysis.shingle.ShingleFilter
import org.apache.lucene.document.{ DoublePoint, FieldType }
import org.apache.lucene.index.{ FieldInvertState, IndexOptions, IndexWriter, IndexWriterConfig, Term }
import org.apache.lucene.search.{ BooleanClause, BooleanQuery, BoostQuery, FuzzyQuery, Query, TermQuery }
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.store.Directory

import LuceneUtil.tokenIter
import au.csiro.data61.gnaf.util.Gnaf.D61_NO_NUM
import au.csiro.data61.gnaf.util.Util.getLogger

/**
 * GNAF specific field names, analyzers and scoring for Lucene.
 */
object GnafLucene {
  val log = getLogger(getClass)
  
  /** GNAF Lucene field names */
  val F_JSON = "json"
  val F_LOCATION = "location"
  val F_D61ADDRESS = "d61Address"
  val F_D61ADDRESS_NOALIAS = "d61AddressNoAlias"
  
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
  
  /** tf-idf doesn't work well with addresses, so disable  tf, idf and length norm */
  object AddressSimilarity extends ClassicSimilarity {
    override def lengthNorm(state: FieldInvertState) = state.getBoost // no length norm, don't penalize multiple aliases
    override def tf(freq: Float): Float = 1.0f // don't boost street and locality name being the same
    override def idf(docFreq: Long, docCount: Long): Float = 1.0f // don't penalize SMITH STREET for being common
    // coord factor boosts docs that match more query terms, so correct match scores higher than spuriously same street and locality name
  }

  val storedNotIndexedFieldType = {
    val t = new FieldType
    // copied from StringField
    t.setOmitNorms(true);
    t.setStored(true);
    t.setTokenized(false);
    t.setIndexOptions(IndexOptions.NONE); // StringField has DOCS
    t.freeze();
    t
  }
  
  val d61AddrFieldType = {
    val t = new FieldType
    // copied from TextField
    t.setStored(true);
    t.setTokenized(true);
    t.setIndexOptions(IndexOptions.DOCS); // TextField has DOCS_AND_FREQS_AND_POSITIONS
    t.setOmitNorms(true); // AddressSimilarity not using norms
    t.freeze();
    t
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
  
  def mkIndexer(dir: Directory) = new IndexWriter(
    dir,
    new IndexWriterConfig(shingleWhiteLowerAnalyzer)
      .setOpenMode(IndexWriterConfig.OpenMode.CREATE)
      .setSimilarity(AddressSimilarity)
  )
  
  case class FuzzyParam(
      /** max number of edits permitted for a match (0 for no fuzzy matching) */
      maxEdits: Int, 
      /** fuzzy matching only applied to terms of at least this length */
      minLength: Int,
      /** the initial length that must match exactly before fuzzy matching is applied to the remainder */
      prefixLength: Int
  )
  
  case class BoundingBox(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double) {
    def toQuery = DoublePoint.newRangeQuery(F_LOCATION, Array[Double](minLat, minLon), Array[Double](maxLat, maxLon))
  }
  
  case class QueryParam(
      /** address search terms - best results if ordered: site/building name, unit/flat, level, street, locality, state abbreviation, postcode */
      addr: String,
      /** number of search results to return */
      numHits: Int,
      /** optional fuzzy matching */
      fuzzy: Option[FuzzyParam], 
      /** optional filtering by a bounding box (addr may be blank) */
      box: Option[BoundingBox]
    ) {
    def toQuery: Query = {
      val q = tokenIter(shingleWhiteLowerAnalyzer, F_D61ADDRESS, addr).foldLeft {
        val b = new BooleanQuery.Builder
        // small score increment for addresses with no number (smaller than for a number match)
        b.add(new BooleanClause(new BoostQuery(new TermQuery(new Term(F_D61ADDRESS, D61_NO_NUM)), 0.1f), BooleanClause.Occur.SHOULD))
        box.foreach(x => b.add(new BooleanClause(x.toQuery, BooleanClause.Occur.FILTER)))
        b.setMinimumNumberShouldMatch(2) // could be D61_NO_NUM and 1 user term or 2 user terms
        b
      }{ (b, t) =>
        val q = {
          val term = new Term(F_D61ADDRESS, t)
          val q = fuzzy
            .filter(f => f.maxEdits > 0 && t.length >= f.minLength)
            .map(f => new FuzzyQuery(term, f.maxEdits, f.prefixLength))
            .getOrElse(new TermQuery(term))
          val n = shingleSize(t)
          if (n < 2) q else new BoostQuery(q, Math.pow(3.0, n-1).toFloat)
        }
        b.add(new BooleanClause(q, BooleanClause.Occur.SHOULD))
      }.build
      log.debug(s"mkQuery: bool query = ${q.toString(F_D61ADDRESS)}")
      q
    }
  }
  
}