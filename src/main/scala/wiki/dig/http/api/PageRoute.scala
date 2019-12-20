package wiki.dig.http.api

import io.circe.syntax._
import spark.Spark._
import spark.{Request, Response, Route}
import wiki.dig.store.db.{PageContentDb, PageDb}
import wiki.dig.util.Logging

object PageRoute extends JsonSupport with Logging {
  def register(): Unit = {
    //获取账号根据邮箱
    get("/wiki/page", "text/html", showPageHtml)
    get("/wiki/page_content", "text/html", showPageContent)
  }

  private def showPageContent: Route = (request: Request, _: Response) => {
    Option(request.queryMap("id").value()).flatMap(_.toIntOption) match {
      case Some(id) =>
        PageContentDb.getContent(id).getOrElse("")
      case None =>
        ""
    }
  }

  def showPageHtml: Route = (request: Request, _: Response) => {
    //val name = Option(request.params(":name")).getOrElse("").trim
    val name = Option(request.queryMap("name").value()).getOrElse("").trim
    PageDb.getIdByName(name) match {
      case Some(pageId) =>
        val inlinks = PageDb.getInlinks(pageId).flatMap {
          id =>
            PageDb.getNameById(id).map {
              linkName =>
                s"""<li><a href="?name=${linkName}">${linkName}</a></li>"""
            }
        }.mkString("<ul>", "\n", "</ul>")

        val outlinks = PageDb.getInlinks(pageId).flatMap {
          id =>
            PageDb.getNameById(id).map {
              linkName =>
                s"""<li><a href="?name=${linkName}">${linkName}</a></li>"""
            }
        }.mkString("<ul>", "\n", "</ul>")

        s"""
           |<html>
           |<head><title>${name}</title></head>
           |<body>
           |  <h2>Inlinks:</h2>
           |  ${inlinks}
           |  <h2>Outlinks:</h2>
           |  ${outlinks}
           |</body>
           |""".stripMargin
    }
  }

  def showPageJson: Route = (request: Request, _: Response) => {
    val name = Option(request.params(":name")).getOrElse("").trim
    PageDb.getIdByName(name) match {
      case Some(pageId) =>
        val inlinks = PageDb.getInlinks(pageId).flatMap {
          id =>
            PageDb.getNameById(id).map(linkName => (id.asJson, linkName.asJson))
        }.asJson

        val outlinks = PageDb.getInlinks(pageId).flatMap {
          id =>
            PageDb.getNameById(id).map(linkName => (id.asJson, linkName.asJson))
        }.asJson

        jsonOk(
          Map("id" -> pageId.asJson,
            "name" -> name.asJson,
            "inlinks" -> inlinks.asJson,
            "outlinks" -> outlinks.asJson
          ).asJson
        )
      case None =>
        jsonError(s"词条不存在：$name")
    }
  }
}
