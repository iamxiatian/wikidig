package wiki.digger.db

import java.io.File

/**
  * 维基百科的文章数据库，采用RocksDB保存
  *
  * @author Tian Xia
  *         School of IRM, Renmin University of China.
  *         Oct 10, 2017 16:26
  */
object ArticleDb extends KeyValueDb("article", new File("db/article")) {

  def generateFromRawXml(f: File): Unit = {

  }


  def main(args: Array[String]): Unit = {
    println("hello world")

    put("name", "xiatian")
    put("name", "xiatian")

    println(get("name"))
  }


}
