package wiki.dig.db

import java.io._
import java.nio.charset.StandardCharsets

import com.google.common.collect.Lists
import com.google.common.io.Files
import org.apache.commons.lang3.StringUtils
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

  def build(startId: Int = 1, batchSize: Int = 1000) = {
    val maxId = Await.result(PageRepo.maxId(), Duration.Inf).get
    //val maxId = 58046434 //最大的id

    var fromId = startId

    while (fromId < maxId) {
      println(s"process $fromId / $maxId ...")
      val pages = Await.result(PageRepo.listFromId(fromId, batchSize), Duration.Inf)
      pages.foreach {
        page =>
          saveContent(page.id, page.text)
          fromId = page.id
      }

      fromId = fromId + 1
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
    print(s"==> Close $dbName ... ")
    cfHandlers.forEach(h => h.close())
    db.close()
    println("DONE.")
  }

  def output(ids: Iterator[String], outFile: File): Unit = {
    val writer = Files.newWriter(outFile, UTF_8)
    ids.filter(s => s.nonEmpty && StringUtils.isNumeric(s))
      .zipWithIndex
      .foreach {
        case (id, idx) =>
          val text = getContent(id.toInt).map(_.replaceAll("\n", "   ")).getOrElse("")
          writer.write(s"$id\t$text\n")
          if (idx % 1000 == 0) {
            LOG.info(s"processing at line $idx")
          }
      }
    writer.close()
    LOG.info("output completed.")
  }
}