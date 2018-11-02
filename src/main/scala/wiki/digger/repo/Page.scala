package wiki.digger.repo

import wiki.digger.repo.core.Repo

import scala.concurrent.Future


case class Page(id: Long,
                pageId: Int,
                name: String,
                text: String,
                isDisambiguation: Boolean)

object PageRepo extends Repo[Page] {

  import profile.api._

  class PageTable(tag: Tag) extends
    Table[Page](tag, "Page") {

    def id = column[Long]("id", O.PrimaryKey)

    def pageId = column[Int]("pageId")

    def name = column[String]("name")

    def text = column[String]("text")

    def isDisambiguation = column[Boolean]("isDisambiguation")

    def * = (id, pageId, name, text, isDisambiguation) <> (Page.tupled, Page.unapply)
  }

  val pages = TableQuery[PageTable]

  def findByPageId(pageId: Int): Future[Option[Page]] = db.run {
    pages.filter(_.pageId === pageId).result.headOption
  }

  def findByName(name: String): Future[Option[Page]] = db.run {
    pages.filter(_.name === name).result.headOption
  }

}