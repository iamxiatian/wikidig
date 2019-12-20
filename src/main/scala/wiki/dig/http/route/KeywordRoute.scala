package wiki.dig.http.route

import io.circe.syntax._
import ruc.irm.extractor.keyword.TextRankExtractor
import ruc.irm.extractor.keyword.TextRankExtractor.GraphType
import ruc.irm.extractor.keyword.TextRankExtractor.GraphType.{PositionDivRank, PositionRank}
import spark.Spark._
import spark.{Request, Response, Route}
import wiki.dig.algorithm.keyword.ArticleDataset
import wiki.dig.util.Logging

import scala.jdk.CollectionConverters._

object KeywordRoute extends JsonSupport with Logging {
  val weightedExtractor: TextRankExtractor = new TextRankExtractor(PositionRank)
  //  val ningExtractor: TextRankExtractor = new TextRankExtractor(NingJianfei)
  //  val clusterExtractor: TextRankExtractor = new TextRankExtractor(ClusterRank)

  val weightedDivExtractor: TextRankExtractor = new TextRankExtractor(PositionDivRank)
  //  val clusterDivExtractor: TextRankExtractor = new TextRankExtractor(ClusterDivRank)

  def register(): Unit = {
    //获取账号根据邮箱
    get("/keyword/extract.do", "application/json", extract)

    get("/keyword/test", "text/html", test)
  }


  private def test: Route = (request: Request, _: Response) => {
    val topN = Option(request.queryMap("id").value()).flatMap(_.toIntOption).getOrElse(5)

    Option(request.queryMap("id").value()).flatMap(_.toIntOption) match {
      case Some(id) =>
        val article = ArticleDataset.getArticle(id)
        val keywords1 = weightedExtractor.extractAsString(article.title, article.content, topN)
        val keywords2 = weightedDivExtractor.extractAsString(article.title, article.content, topN)
        s"""
           |<html>
           |<head><title>测试[id: ${id}]：${article.title}</title></head>
           |<body>
           |<ul>
           |  <li>Title: <a href="${article.url}">${article.title}</a></li>
           |  <li>tags: ${article.tags.mkString(" ")}</li>
           |  <li>WeightRank: ${keywords1}</li>
           |  <li>DivRank: ${keywords2}</li>
           |</ul>
           |<div>${article.content.replaceAll("\n", "<p/>")}</div>
           |</body>
           |</html>
           |""".stripMargin
      case None =>
        s"未指定文章ID"
    }
  }

  private def extract: Route = (request: Request, _: Response) => {
    val title = Option(request.queryMap("title").value()).getOrElse("").trim
    val content = Option(request.queryMap("content").value()).getOrElse("").trim

    val positionRank = new TextRankExtractor(GraphType.PositionRank).extractAsList(title, content, 10)
    jsonOk(positionRank.asScala.map(_.asJson).asJson)
  }
}
