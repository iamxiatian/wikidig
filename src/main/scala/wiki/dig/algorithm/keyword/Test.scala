package wiki.dig.algorithm.keyword

import wiki.dig.http.route.PaperRoute.weightedExtractor

object Test {
  def entropy(dist: Seq[Int]): Double = {
    val sum = dist.sum
    val probabilities = dist.map(_ * 1.0 / sum)

    probabilities.map(p => -p * Math.log(p) / Math.log(2)).sum
  }

  def test(id: Int): Unit = {
    val paper = PaperDataset.get(id)
    println(s"${paper.title}:\n\t\t${paper.tags.mkString(";")}")
    val keywords1 = weightedExtractor.extractAsString(paper.title, paper.`abstract`, 10)
  }

  def main(args: Array[String]): Unit = {
    (1 to 10).foreach {
      id =>
        println("------------------")
        test(id)
    }
    val article = ArticleDataset.getArticle(2)
    println(s"${article.title}:\n\t\t${article.tags.mkString(";")}")
    val keywords1 = weightedExtractor.extractAsString(article.title, article.content, 10)

  }
}
