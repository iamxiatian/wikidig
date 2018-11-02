package wiki.digger.repo.core

import wiki.digger.common.MyConf

object RepoProvider {
  import slick.jdbc.MySQLProfile.api._

  val mysqlDb: Database = Database.forConfig("wiki.db.mysql", MyConf.config)

  def close(): Unit = {
    if (mysqlDb != null)
      mysqlDb.close()
  }
}
