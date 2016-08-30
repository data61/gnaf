package au.csiro.data61.gnaf.lucene.util

import java.io.{ Closeable, File }

import scala.util.Try

import org.apache.lucene.analysis.{ Analyzer, TokenStream }
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document.{ Document, Field, FieldType, SortedDocValuesField }
import org.apache.lucene.index.{ DirectoryReader, IndexOptions }
import org.apache.lucene.index.{ IndexWriter, IndexWriterConfig }
import org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS
import org.apache.lucene.search.{ IndexSearcher, Query, ScoreDoc, Sort }
import org.apache.lucene.store.{ Directory, FSDirectory }
import org.apache.lucene.util.BytesRef

import au.csiro.data61.gnaf.util.Timer
import au.csiro.data61.gnaf.util.Util.getLogger


/**
 * Non GNAF specific code for Lucene indexing and searching.
 * 
 * simplified from: https://github.csiro.au/bac003/social-watch/blob/master/analytics/src/main/scala/org/t3as/socialWatch/analytics/LuceneUtil.scala
 * removed highlighting and query parsing from Searching
 * removed term freq utils, suggesting, analyzing
 */
object LuceneUtil {

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
    
  object Indexing {
    /** Create an indexing FieldType. Default suitable for keyword (untokenized) fields */
    def mkFieldType(tokenized: Boolean = false, stored: Boolean = true, indexed: Boolean = true, termVectors: Boolean = false, opt: IndexOptions = DOCS_AND_FREQS) = {
      val t = new FieldType
      t.setTokenized(tokenized)
      t.setIndexOptions(opt)
      t.setStored(stored)
      t.setStoreTermVectors(termVectors)
      t.freeze()
      t
    }
  
    /** Facilitate populating a Lucene Document.
     *  @return (Lucene doc, function to add a field to the doc) 
     */
    def docAdder(fieldType: String => FieldType) = {
      val d = new Document
      (d, (k: String, v: String, sorted: Boolean) => if (!v.isEmpty()) {
        d.add(new Field(k, v, fieldType(k)))
        if (sorted) d.add(new SortedDocValuesField(k, new BytesRef(v)))
      })
    }

    class Indexer(indexDir: File, create: Boolean, analyzer: Analyzer) extends Closeable {
      val log = getLogger(getClass)
  
      val writer = indexWriter  
      protected def indexWriter = new IndexWriter(directory, indexWriterConfig)
      protected def directory: Directory = FSDirectory.open(indexDir.toPath)
      protected def indexWriterConfig = {
        import IndexWriterConfig.OpenMode._
        val c = new IndexWriterConfig(analyzer)
        c.setOpenMode(if (create) CREATE else APPEND)
        c
      }
  
      def add(d: Document) = writer.addDocument(d)
  
      def close = {
        log.debug(s"numDocs = ${writer.numDocs}")
        writer.close
      }
      
    }
  }
  
  object Searching {
    class Searcher[Hit, Results](
        indexDir: File,
        toHit: (ScoreDoc, Document) => Hit, // convert score and map of fields to Hit
        toResults: (Int, Float, Seq[Hit], Option[String]) => Results, // convert totalHits, elapsedSecs, Seq[Hit], Option[error] to Results
        toSort: (Option[String], Boolean) => Option[Sort]
    ) extends Closeable {      
      val log = getLogger(getClass)
  
      val searcher = open
      protected def open = new IndexSearcher(DirectoryReader.open(directory))
      protected def directory: Directory = FSDirectory.open(indexDir.toPath)
          
      def search(q: Query, sort: Option[Sort], numHits: Int = 20, firstHit: Int = 0) = {
        val timer = Timer()
        
        val result = for {
          // q <- parseQuery(query)
          topDocs <- Try {
            if (sort.isDefined) searcher.search(q, numHits + firstHit, sort.get, true, true)
            else searcher.search(q, numHits + firstHit)
          }
          hits <- Try {
            topDocs.scoreDocs.slice(firstHit, numHits + firstHit) map { scoreDoc =>
              toHit(scoreDoc, searcher.doc(scoreDoc.doc))
            }
          }
        } yield toResults(topDocs.totalHits, timer.elapsedSecs.toFloat, hits, None)
        
        result.recover { case e => toResults(0, timer.elapsedSecs.toFloat, List(), Some(e.getMessage)) }.get
      }
      
      def close = searcher.getIndexReader.close
      
    }
    
  }

}