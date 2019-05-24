package wiki.dig.algorithm.tfidf

import java.io.StringReader

import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

import scala.collection.mutable

object LuceneUtil {

  def tokenize(s: String): Seq[String] = {
    println(s)
    val terms = mutable.ListBuffer.empty[String]
    val analyzer = new EnglishAnalyzer();

    val stream = analyzer.tokenStream(null, new StringReader(s))
    stream.reset()
    while (stream.incrementToken()) {
      val term = stream.getAttribute(classOf[CharTermAttribute]).toString()
      println(term)
      terms += term
    }
    stream.close()

    terms.toSeq
  }

  def main(args: Array[String]): Unit = {
    val s = "hello world, I love china"

    tokenize(s) foreach println
  }

}
