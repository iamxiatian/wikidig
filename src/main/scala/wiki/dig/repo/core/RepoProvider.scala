package wiki.dig.repo.core

import wiki.dig.common.MyConf

object RepoProvider {
  import slick.jdbc.MySQLProfile.api._

  val mysqlDb: Database = Database.forConfig("wiki.db.mysql", MyConf.config)

  def close(): Unit = {
    if (mysqlDb != null)
      mysqlDb.close()
  }
}
