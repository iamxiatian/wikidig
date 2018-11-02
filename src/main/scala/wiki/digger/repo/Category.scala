package wiki.digger.repo

import wiki.digger.repo.core.Repo

import scala.concurrent.Future


case class Category(id: Long,
                    pageId: Int,
                    name: String)

object CategoryRepo extends Repo[Category] {

  import profile.api._

  class CategoryTable(tag: Tag) extends
    Table[Category](tag, "Page") {

    def id = column[Long]("id", O.PrimaryKey)

    def pageId = column[Int]("pageId")

    def name = column[String]("name")

    def * = (id, pageId, name) <> (Category.tupled, Category.unapply)
  }

  val entities = TableQuery[CategoryTable]

  def findById(id: Long): Future[Option[Category]] = db.run {
    entities.filter(_.id === id).result.headOption
  }

  def findByIds(ids: Seq[Long]): Future[Seq[Category]] = db.run {
    entities.filter(_.id.inSet(ids.toSet)).result
  }

  /**
    * 根分类，对于英文来说，根的名称为Contents
    *
    * @return
    */
  def root(): Future[Option[Category]] = db.run {
    entities.filter(_.name === "Main_topic_classifications").result.headOption
  }

  /**
    * 获取第一级有效的分类
    * @return
    */
  def levelOne(): Future[Seq[Category]] = root().flatMap {
    case Some(r) =>
      CategoryOutlinkRepo.findOutlinksById(r.id).flatMap {
        ids => findByIds(ids)
      }
    case None => Future.successful(Seq.empty[Category])
  }
}