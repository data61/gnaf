package au.csiro.data61.gnaf.lucene

import java.io.Closeable
import scala.util.Try
import org.apache.lucene.analysis.{ Analyzer, TokenStream }
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.{ IndexSearcher, Query, ScoreDoc }
import org.apache.lucene.store.Directory
import au.csiro.data61.gnaf.util.Timer
import au.csiro.data61.gnaf.util.Util.getLogger
import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute


/**
 * Non GNAF specific code for Lucene indexing and searching.
 * 
 * simplified from: https://github.csiro.au/bac003/social-watch/blob/master/analytics/src/main/scala/org/t3as/socialWatch/analytics/LuceneUtil.scala
 */
object LuceneUtil {
  val log = getLogger(getClass)

  def tokenIter(ts: TokenStream): Iterator[String] = {
    ts.reset
    Iterator.continually {
      val more = ts.incrementToken
      if (!more) {
        ts.end
        ts.close
      }
      more
    }.takeWhile(identity).map(_ => ts.getAttribute(classOf[CharTermAttribute]).toString)
  }

  def tokenIter(analyzer: Analyzer, fieldName: String, text: String): Iterator[String]
    = tokenIter(analyzer.tokenStream(fieldName, text))
    
  def directory(indexDir: File) = FSDirectory.open(indexDir.toPath)
  
  class Searcher[Hit, Results](
    directory: Directory,
    toHit: (ScoreDoc, Document) => Hit, // convert score and map of fields to Hit
    toResults: (Int, Float, Seq[Hit], Option[String]) => Results // convert totalHits, elapsedSecs, Seq[Hit], Option[error] to Results
  ) extends Closeable {      
    val log = getLogger(getClass)

    val searcher = open
    protected def open = new IndexSearcher(DirectoryReader.open(directory))
    
    log.debug(s"Searcher: numDocs = ${searcher.getIndexReader.numDocs}")
        
    def search(q: Query, numHits: Int = 20) = {
      val timer = Timer()
      
      val result = for {
        topDocs <- Try {
          searcher.search(q, numHits)
        }
        hits <- Try {
          topDocs.scoreDocs map { scoreDoc => toHit(scoreDoc, searcher.doc(scoreDoc.doc)) }
        }
      } yield toResults(topDocs.totalHits, timer.elapsedSecs.toFloat, hits, None)
      
      result.recover { case e => toResults(0, timer.elapsedSecs.toFloat, List(), Some(e.getMessage)) }.get
    }
    
    def close = searcher.getIndexReader.close
  }

}