package wiki.dig.algorithm.tfidf

import scala.collection.mutable

/**
  * TfIdf统计处理
  */
class TfIdf {
  //词项与倒排记录表的集合
  val indexes = mutable.Map.empty[String, Postings]

  def index(doc: IndexDoc): Unit = {
    doc.termGroup foreach {
      case (term, cnt) =>
        index(doc.idx, term, cnt)
    }
  }

  def index(docId: Int, term: String, tf: Int): Unit = {
    val postings: Postings = if (indexes.contains(term)) {
      indexes(term)
    } else {
      val postings = new Postings
      indexes.put(term, postings)
      postings
    }

    postings.incTf(docId, tf)
  }

  def show(): Unit = {
    indexes foreach {
      case (term, postings) =>
        print(s"$term (${postings.df}): ")
        println(postings.tf
          .map {
            case (docId, tf) =>
              s"${docId}/$tf"
          }.mkString(" "))
    }
  }
}


object TfIdf {
  def main(args: Array[String]): Unit = {
    val tfidf = new TfIdf
    val d1 = IndexDoc(1, "hello world, where are you come from?")
    val d2 = IndexDoc(2, "Jesus loves me, this I known")
    tfidf.index(d1)
    tfidf.index(d2)
    tfidf.show()
  }
}
