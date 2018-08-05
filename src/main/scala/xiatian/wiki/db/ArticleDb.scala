package xiatian.wiki.db

import java.io.File

import xiatian.wiki.common.MyConf

/**
  * 维基百科的文章数据库，采用RocksDB保存
  *
  * @author Tian Xia
  *         School of IRM, Renmin University of China.
  *         Oct 10, 2017 16:26
  */
object ArticleDb extends StoreRocksDB(MyConf.articleDbFile) {

  def generateFromRawXml(f: File): Unit ={

  }


  def main(args: Array[String]): Unit = {
    println("hello world")

    put("name", "xiatian")
    put("name", "xiatian")

    println(get("name"))

    shutdown()
  }



}
