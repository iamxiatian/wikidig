package wiki.dig.algorithm.keyword

import wiki.dig.util.Segment

import scala.xml.XML

/**
  * 关键词抽取的测试文章集合
  */
object ArticleDataset {

  val doc = XML.loadFile("data/articles.xml")

  lazy val articles = doc \\ "article"

  /** 文章总数量 */
  lazy val articleCount = articles.length

  /** 所有tags标签下包含的标签数量: 3.565 */
  lazy val averageTagCount = (doc \\ "tags").map(_.text.split(",").length).foldLeft(0)(_ + _) / articleCount.toDouble

  /**
    * 文章的平均字符数量： 2629.101
    */
  lazy val averageContentLength = (doc \\ "content").map(_.text.length).foldLeft(0)(_ + _) / articleCount.toDouble

  /**
    * 文章的平均词语数量: 1631.814
    */
  lazy val averageContentWords = (doc \\ "content").map(e => Segment.segment(e.text).length).foldLeft(0)(_ + _) / articleCount.toDouble

  private val acceptPOS = (pos: String) => pos.take(1) == "n" || pos.take(3) == "adj" || pos.take(1) == "v"

  /**
    * 文章的平均词语数量： 过滤后只保留名词，动词和形容词: 757.284
    */
  lazy val filteredAverageContentWords = (doc \\ "content").map(a => Segment.segment(a.text).filter(b => acceptPOS(b._2)).length).foldLeft(0)(_ + _) / articleCount.toDouble

  /**
    * 获取第id篇文章内容
    */
  def getArticle(id: Int): Article = Article(
    (articles(id) \ "url").text,
    (articles(id) \ "title").text,
    (articles(id) \ "tags").text.split(",").map(_.trim).filter(_.nonEmpty),
    (articles(id) \ "content").text
  )

  def tags(articleId: Int): String = (articles(articleId) \ "tags").text

}

case class Article(url: String,
                   title: String,
                   tags: Seq[String],
                   content: String)
