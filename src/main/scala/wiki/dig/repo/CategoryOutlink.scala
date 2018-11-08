package wiki.dig.repo

import wiki.dig.repo.core.Repo

import scala.concurrent.Future


case class CategoryOutlink(id: Int,
                           outLinks: Int)

object CategoryOutlinkRepo extends Repo[CategoryOutlink] {

  import profile.api._

  class CategoryOutlinkTable(tag: Tag) extends
    Table[CategoryOutlink](tag, "category_outlinks") {

    def id = column[Int]("id", O.PrimaryKey)

    def outLinks = column[Int]("outLinks")

    def * = (id, outLinks) <> (CategoryOutlink.tupled, CategoryOutlink.unapply)
  }

  val entities = TableQuery[CategoryOutlinkTable]

  def findOutlinksById(id: Int): Future[Seq[Int]] = db.run {
    entities.filter(_.id === id).map(_.outLinks).result
  }
}