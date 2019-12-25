package wiki.dig.http.route

import io.circe.syntax._
import org.zhinang.conf.Configuration
import ruc.irm.extractor.keyword.TextRankExtractor
import ruc.irm.extractor.keyword.TextRankExtractor.GraphType
import ruc.irm.extractor.keyword.TextRankExtractor.GraphType.{PositionDivRank, PositionRank}
import ruc.irm.extractor.nlp.SegmentFactory
import spark.Spark._
import spark.{Request, Response, Route}
import wiki.dig.algorithm.keyword.PaperDataset
import wiki.dig.util.Logging

import scala.jdk.CollectionConverters._

object PaperRoute extends JsonSupport with Logging {
  val weightedExtractor: TextRankExtractor = new TextRankExtractor(PositionRank)
  //  val ningExtractor: TextRankExtractor = new TextRankExtractor(NingJianfei)
  //  val clusterExtractor: TextRankExtractor = new TextRankExtractor(ClusterRank)

  val weightedDivExtractor: TextRankExtractor = new TextRankExtractor(PositionDivRank)
  //  val clusterDivExtractor: TextRankExtractor = new TextRankExtractor(ClusterDivRank)

  def register(): Unit = {
    //获取账号根据邮箱
    get("/paper/extract.do", "application/json", extract)

    get("/paper/test", "text/html", test)

    get("/paper/graph", "text/html", getWordGraph)

    get("/paper/show", "text/html", show)

    get("/paper/list", "text/html", list)
  }

  lazy val allResults = PaperDataset.papers.zipWithIndex.map {
    case (paper, idx) =>
      val keywords: Seq[String] = weightedExtractor.extractAsList(paper.title, paper.`abstract`, 10).asScala.toSeq
      val tags = paper.tags

      //抽取结果中，tags至少包含一个
      val existedOne = keywords.exists(tags.contains(_))
      val indicator = if (existedOne) "GOOD" else "BAD"
      s"""
         |[$indicator] $idx: <a href="/paper/show?id=$idx" target="_blank">${paper.title}</a><br/>
         |tags: ${paper.tags.mkString("; ")}<br/>
         |keywords: ${keywords.mkString("; ")}<br/>
         |""".stripMargin
  }.mkString("<div>", "\n<hr/>", "</div>")

  /**
    * 抽取完全失败的文章列表
    *
    * @return
    */
  def list: Route = (request: Request, _: Response) => {
    allResults
  }


  /**
    * 显示文章内容
    *
    * @return
    */
  def show: Route = (request: Request, _: Response) => {
    val topN = Option(request.queryMap("topN").value()).flatMap(_.toIntOption).getOrElse(5)

    Option(request.queryMap("id").value()).flatMap(_.toIntOption) match {
      case Some(id) =>
        val paper = PaperDataset.get(id)
        val keywords1 = weightedExtractor.extractAsString(paper.title, paper.`abstract`, topN)
        val keywords2 = weightedDivExtractor.extractAsString(paper.title, paper.`abstract`, topN)

        //显示文本分词后的结果
        val words = SegmentFactory.getSegment(new Configuration()).tag(paper.`abstract`).asScala


        s"""
           |<html><head><title>${paper.title}</title></head>
           |<body>
           |  <h2>${paper.title}</h2>
           |  <ul>
           |  <li>tags: ${paper.tags.mkString("; ")}</li>
           |  <li>WeightRank: ${keywords1}</li>
           |  <li>DivRank: ${keywords2}</li>
           |  </ul>
           |  <div>
           |  ${paper.`abstract`.replaceAll("\n", "<br/>")}
           |  </div>
           |  <hr/>
           |  <div>
           |  ${words.map(_.toString).mkString(" ")}
           |  </div>
           |</body></html>
           |""".stripMargin
      case None =>
        "未指定参数id"
    }
  }

  /**
    * 把指定id文章的词图，转成dot文件，方便查看。
    *
    * @return
    */
  def getWordGraph: Route = (request: Request, _: Response) => {
    Option(request.queryMap("id").value()).flatMap(_.toIntOption) match {
      case Some(id) =>
        PaperDataset.toDotFile(id, s"./www/dot2/${id}.dot")
        s"""<a href="/dot2/${id}.dot">下载dot文件</a>
           |<a href="/dot2/${id}.png">下载png文件</a>
           |""".stripMargin
      case None =>
        "未指定参数id"
    }
  }

  /**
    * 测试指定id文章的关键词抽取结果
    *
    * @return
    */
  private def test: Route = (request: Request, _: Response) => {
    val topN = Option(request.queryMap("topN").value()).flatMap(_.toIntOption).getOrElse(5)

    Option(request.queryMap("id").value()).flatMap(_.toIntOption) match {
      case Some(id) =>
        val paper = PaperDataset.get(id)
        val keywords1 = weightedExtractor.extractAsString(paper.title, paper.`abstract`, topN)
        val keywords2 = weightedDivExtractor.extractAsString(paper.title, paper.`abstract`, topN)
        s"""
           |<html>
           |<head><title>测试[id: ${id}]：${paper.title}</title></head>
           |<body>
           |<ul>
           |  <li>Title: ${paper.title}</li>
           |  <li>tags: ${paper.tags.mkString(" ")}</li>
           |  <li>WeightRank: ${keywords1}</li>
           |  <li>DivRank: ${keywords2}</li>
           |</ul>
           |<div>${paper.`abstract`.replaceAll("\n", "<p/>")}</div>
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
