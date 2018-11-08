package wiki.dig.repo

import wiki.dig.repo.core.Repo

import scala.concurrent.Future


case class CategoryPage(id: Int,
                        pageId: Int)

object CategoryPageRepo extends Repo[CategoryPage] {

  import profile.api._

  class CategoryPagesTable(tag: Tag) extends
    Table[CategoryPage](tag, "category_pages") {

    def id = column[Int]("id", O.PrimaryKey)

    def pageId = column[Int]("pages")

    def * = (id, pageId) <> (CategoryPage.tupled, CategoryPage.unapply)
  }

  val entities = TableQuery[CategoryPagesTable]

  def findPagesById(id: Int): Future[Seq[Int]] = db.run {
    entities.filter(_.id === id).map(_.pageId).result
  }
}