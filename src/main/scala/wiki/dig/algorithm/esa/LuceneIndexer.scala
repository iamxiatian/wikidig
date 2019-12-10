package wiki.dig.algorithm.esa

import java.nio.file.Path

import better.files.File
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.document.{Document, Field, FieldType, StringField}
import org.apache.lucene.index.{IndexOptions, IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory

class LuceneIndexer(indexDir: Path) {
  val analyzer = new EnglishAnalyzer()
  val config = new IndexWriterConfig(analyzer)
  config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)

  val directory = FSDirectory.open(indexDir)

  indexDir.toFile.mkdirs()
  val writer = new IndexWriter(directory, config)


  val contentFieldType = new FieldType()
  contentFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS)
  contentFieldType.setStored(true)
  contentFieldType.setStoreTermVectors(true)
  contentFieldType.setTokenized(true)

  def index(id: Int,
            title: String,
            text: String
           ): Unit = {
    val doc = new Document()
    doc.add(new StringField("id", id.toString, Field.Store.YES))
    val field = new Field("content", title + "\n" + text, contentFieldType)
    doc.add(field)
    writer.addDocument(doc)
  }

  def close(): Unit = {
    writer.close()
  }
}

object LuceneIndexer {
  def main(args: Array[String]): Unit = {
    val indexer = new LuceneIndexer(File("./index/test").path)
    indexer.index(66, "I love china", "trade war")
    indexer.index(88, "china work", "great")
    indexer.close()
  }
}