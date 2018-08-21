package wiki.digger.db.dao

import wiki.digger.common.MyConf

object RepoProvider {
  import slick.jdbc.MySQLProfile.api._

  val mysqlDb: Database = Database.forConfig("digger.db.mysql", MyConf.config)

  def close(): Unit = {
    if (mysqlDb != null)
      mysqlDb.close()
  }
}
