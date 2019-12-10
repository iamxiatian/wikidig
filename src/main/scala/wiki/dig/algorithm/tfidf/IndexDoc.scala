package wiki.dig.algorithm.tfidf

/**
  * 表示一个待索引文档
  *
  * @param idx
  * @param terms
  */
class IndexDoc(val idx: Int, val terms: Seq[String]) {
  def termGroup: Map[String, Int] = {
    terms.groupBy(identity).mapValues(_.size)
  }
}

object IndexDoc {
  def apply(idx: Int, text: String): IndexDoc = {
    new IndexDoc(idx, LuceneUtil.tokenize(text))
  }
}
