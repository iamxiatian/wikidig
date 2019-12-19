package wiki.dig.http.api

import spark.{Request, Response, Route}
import wiki.dig.util.Logging
import spark.Spark._
import wiki.dig.store.db.{PageContentDb, PageDb}
import io.circe.syntax._

object PageRoute extends JsonSupport with Logging {
  def register(): Unit = {
    //获取账号根据邮箱
    get("/api/wiki/page/:name", "application/json", showPage)
  }


  private def showPage: Route = (request: Request, _: Response) => {
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
    val inlinks = PageDb.getInlinks(pageId)
    AccountRepo.findByEmail(email) match {
      case Some(account) =>
        jsonOk(account.toJson)
      case None =>
        LOG.error(s"email: $email ")
        jsonError("用户邮箱不存在！.")
    }
  }
}
