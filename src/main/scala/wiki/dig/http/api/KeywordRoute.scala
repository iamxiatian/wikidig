package wiki.dig.http.api

import io.circe.syntax._
import ruc.irm.extractor.keyword.TextRankExtractor
import ruc.irm.extractor.keyword.TextRankExtractor.GraphType
import spark.Spark._
import spark.{Request, Response, Route}
import wiki.dig.util.Logging

import scala.jdk.CollectionConverters._

object KeywordRoute extends JsonSupport with Logging {
  def register(): Unit = {
    //获取账号根据邮箱
    get("/api/keyword/extract.do", "application/json", extract)
  }


  private def extract: Route = (request: Request, _: Response) => {
    val title = Option(request.queryMap("title").value()).getOrElse("").trim
    val content = Option(request.queryMap("content").value()).getOrElse("").trim

    val positionRank = new TextRankExtractor(GraphType.PositionRank).extractAsList(title, content, 10)
    jsonOk(positionRank.asScala.map(_.asJson).asJson)
  }
}
