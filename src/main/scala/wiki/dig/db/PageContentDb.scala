package wiki.dig.db

import java.io._
import java.nio.charset.StandardCharsets

import com.google.common.collect.Lists
import org.rocksdb._
import org.slf4j.LoggerFactory
import org.zhinang.util.GZipUtils
import wiki.dig.common.MyConf
import wiki.dig.db.ast.Db
import wiki.dig.repo._
import wiki.dig.util.ByteUtil

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * 把Page的内容信息保存到RocksDB数据库中，里面记录的信息包括：
  */
object PageContentDb extends Db {
  val LOG = LoggerFactory.getLogger(this.getClass)

  import StandardCharsets.UTF_8

  val dbPath = new File(MyConf.dbRootDir, "page/content")
  if (!dbPath.getParentFile.exists())
    dbPath.getParentFile.mkdirs()


  RocksDB.loadLibrary()

  val options = new DBOptions().setCreateIfMissing(!MyConf.pageDbReadOnly)
    .setMaxBackgroundCompactions(10)
    .setCreateMissingColumnFamilies(!MyConf.pageDbReadOnly)

  protected val cfNames = Lists.newArrayList[ColumnFamilyDescriptor](
    new ColumnFamilyDescriptor("default".getBytes(UTF_8))
  )

  protected val cfHandlers = Lists.newArrayList[ColumnFamilyHandle]

  protected val db = RocksDB.open(options, dbPath.getAbsolutePath, cfNames, cfHandlers)

  protected val defaultHandler: ColumnFamilyHandle = cfHandlers.get(0)

  def build(startPage: Int = 1, pageSize: Int = 500) = {
    val count = Await.result(PageRepo.count(), Duration.Inf)
    val pageNum = count / pageSize + 1
    (startPage to pageNum) foreach {
      p =>
        println(s"process $p / $pageNum ...")
        val pages = Await.result(PageRepo.list(p, pageSize), Duration.Inf)

        pages.foreach {
          page =>
            saveContent(page.id, page.text)
        }
    }

    println("DONE")
  }

  private def saveContent(id: Int, content: String) = {
    val key = ByteUtil.int2bytes(id)
    val value = GZipUtils.compress(content.getBytes(UTF_8))
    //    val value = content.getBytes(UTF_8)
    db.put(defaultHandler, key, value)
  }

  def getContent(id: Int): Option[String] = Option(
    db.get(defaultHandler, ByteUtil.int2bytes(id))
  ) match {
    case Some(bytes) =>
      val unzipped = GZipUtils.decompress(bytes)
      Option(new String(unzipped, UTF_8))

    case None => None
  }

  /**
    * 数据库名字
    */
  def dbName: String = "Page Content DB"

  override def close(): Unit = {
    print(s"==> Close Page Db ... ")
    cfHandlers.forEach(h => h.close())
    db.close()
    println("DONE.")
  }
}