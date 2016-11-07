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
import au.csiro.data61.gnaf.util.Util.getLogger
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.similarities.PerFieldSimilarityWrapper

/**
 * GNAF specific field names, analyzers and scoring for Lucene.
 */
object GnafLucene {
  val log = getLogger(getClass)
  
  /** GNAF Lucene field names */
  val F_JSON = "json"
  val F_LOCATION = "location"
  val F_ADDRESS = "address"
  val F_ADDRESS_NOALIAS = "addressNoAlias"
  val F_MISSING_DATA = "noData"
  
  val MISSING_DATA_TOKEN = "N" // store this token in F_MISSING_DATA once for each missing: site/building, flat, level, streetNum
  
  val BIGRAM_SEPARATOR = "~"
    
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
  def shingleSize(s: String) = countOccurrences(s, BIGRAM_SEPARATOR) + 1
  
  /**
   * gnaf-test shows tf-idf doesn't work well with addresses
   * For F_DADDRESS disable tf, idf and length norm,
   * but for F_MISSING_DATA keep tf to favour multiple MISSING_DATA_TOKENs.
   */
  class MissingDataSimilarity extends ClassicSimilarity {
    // default tf - boost repeated MISSING_DATA_TOKEN tokens
    override def lengthNorm(state: FieldInvertState) = state.getBoost // no length norm, don't penalize multiple MISSING_DATA_TOKENs or multiple aliases
    override def idf(docFreq: Long, docCount: Long): Float = 1.0f // don't penalize MISSING_DATA_TOKEN or SMITH STREET for being common
  }
  class AddressSimilarity extends MissingDataSimilarity {
    override def tf(freq: Float): Float = 1.0f // don't boost street and locality name being the same
  }
  val classicSimilarity = new ClassicSimilarity
  object GnafSimilarity extends PerFieldSimilarityWrapper(classicSimilarity) {
    val md = new MissingDataSimilarity
    val addr = new AddressSimilarity
    override def get(name: String) = if (name == F_ADDRESS) addr else if (name == F_MISSING_DATA) md else classicSimilarity
  }

  val storedNotIndexedFieldType = {
    val t = new FieldType
    // based on StringField
    t.setOmitNorms(true);
    t.setStored(true);
    t.setTokenized(false);
    t.setIndexOptions(IndexOptions.NONE); // StringField has DOCS
    t.freeze();
    t
  }
  
  val addressFieldType = {
    val t = new FieldType
    // based on TextField
    t.setOmitNorms(true);
    t.setStored(true);
    t.setTokenized(true);
    t.setIndexOptions(IndexOptions.DOCS); // not using term freq, TextField has DOCS_AND_FREQS_AND_POSITIONS
    t.freeze();
    t
  }
  
  val flatStreetNumFieldType = {
    val t = new FieldType
    t.setOmitNorms(true);
    t.setStored(false);
    t.setTokenized(false);
    t.setIndexOptions(IndexOptions.DOCS);
    t.freeze();
    t
  }
  
  val missingDataFieldType = {
    val t = new FieldType
    t.setOmitNorms(true);
    t.setStored(false);
    t.setTokenized(false);
    t.setIndexOptions(IndexOptions.DOCS_AND_FREQS); // using term freq
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
      result.setTokenSeparator(BIGRAM_SEPARATOR) // default is " ", changed so we can explicitly add a bigram by passing "a~b" through the tokenizer
      new TokenStreamComponents(source, result)
    }
    
    override def getPositionIncrementGap(fieldName: String): Int = 100 // stop shingles matching across boundaries
  }
  
  def mkIndexer(dir: Directory) = new IndexWriter(
    dir,
    new IndexWriterConfig(shingleWhiteLowerAnalyzer)
      .setOpenMode(IndexWriterConfig.OpenMode.CREATE)
      .setSimilarity(GnafSimilarity)
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
      val q = tokenIter(shingleWhiteLowerAnalyzer, F_ADDRESS, addr).foldLeft {
        val b = new BooleanQuery.Builder
        // small score increment for missing: build/site, flat, level, streetNo (smaller than for an actual match)
        b.add(new BooleanClause(new BoostQuery(new TermQuery(new Term(F_MISSING_DATA, MISSING_DATA_TOKEN)), 0.05f), BooleanClause.Occur.SHOULD))
        box.foreach(x => b.add(new BooleanClause(x.toQuery, BooleanClause.Occur.FILTER)))
        if (addr.trim.isEmpty)
          // mobile use case: all addresses in box around me
          b.add(new BooleanClause(new MatchAllDocsQuery, BooleanClause.Occur.SHOULD))
        else
          b.setMinimumNumberShouldMatch(2) // could be MISSING_DATA_TOKEN and 1 user term or 2 user terms
        b
      }{ (b, t) =>
        val q = {
          val term = new Term(F_ADDRESS, t)
          val q = fuzzy
            .filter(f => f.maxEdits > 0 && t.length >= f.minLength)
            .map(f => new FuzzyQuery(term, f.maxEdits, f.prefixLength))
            .getOrElse(new TermQuery(term))
          val n = shingleSize(t)
          if (n < 2) q else new BoostQuery(q, Math.pow(3.0, n-1).toFloat)
        }
        b.add(new BooleanClause(q, BooleanClause.Occur.SHOULD))
      }.build
      log.debug(s"mkQuery: bool query = ${q.toString(F_ADDRESS)}")
      q
    }
  }
  
}