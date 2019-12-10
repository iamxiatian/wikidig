package wiki.dig.algorithm.esa

import java.nio.file.Path

import better.files.File
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.index._
import org.apache.lucene.store.FSDirectory

class LuceneReader(indexDir: Path) {
  val analyzer = new EnglishAnalyzer()
  val config = new IndexWriterConfig(analyzer)
  config.setOpenMode(IndexWriterConfig.OpenMode.APPEND)

  private val SPECIAL_CHARS = Array[Char]('.', '"', '\'', ']', '[', '%', '@', '!', '}', '{', '|')

  val reader: IndexReader = DirectoryReader.open(FSDirectory.open(indexDir))

  def tfidf(): Unit = {
    val terms = MultiTerms.getTerms(reader, "content")
    val enum = terms.iterator()
    var stop = false

    while (!stop) {
      val byteRef = enum.next()
      if (byteRef == null) {
        stop = true
      } else {
        val term = byteRef.utf8ToString()
        
      }
    }
  }

  def close() = {
    reader.close()
  }
}

object LuceneReader {
  def main(args: Array[String]): Unit = {
    val reader = new LuceneReader(File("./index/test").path)
    reader.tfidf()
    reader.close()
  }
}
