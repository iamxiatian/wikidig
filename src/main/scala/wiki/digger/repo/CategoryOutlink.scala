package wiki.digger.repo

import wiki.digger.repo.core.Repo

import scala.concurrent.Future


case class CategoryOutlink(id: Long,
                           outLinks: Long)

object CategoryOutlinkRepo extends Repo[CategoryOutlink] {

  import profile.api._

  class CategoryOutlinkTable(tag: Tag) extends
    Table[CategoryOutlink](tag, "category_outlinks") {

    def id = column[Long]("id", O.PrimaryKey)

    def outLinks = column[Long]("outLinks")

    def * = (id, outLinks) <> (CategoryOutlink.tupled, CategoryOutlink.unapply)
  }

  val entities = TableQuery[CategoryOutlinkTable]

  def findOutlinksById(id: Long): Future[Seq[Long]] = db.run {
    entities.filter(_.id === id).map(_.outLinks).result
  }
}