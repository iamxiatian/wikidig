package wiki.dig.db

import java.io._
import java.nio.charset.StandardCharsets

import com.google.common.collect.Lists
import org.rocksdb._
import org.slf4j.LoggerFactory
import wiki.dig.common.MyConf
import wiki.dig.db.ast.Db
import wiki.dig.repo.{CategoryInlinkRepo, CategoryOutlinkRepo, CategoryPageRepo, CategoryRepo}
import wiki.dig.util.ByteUtil

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * 把Category的信息保存到RocksDB数据库中，里面记录的信息包括：
  *
  * 类别的id:name双向映射关系，来自于数据库的Categoryb.
  *
  * 类别的入链，来自于category_inlinks
  *
  * 类别的出链，来自于category_outlinks
  *
  * 类别指向的页面，来自于category_pages.
  */
object CategoryDb extends Db {
  val LOG = LoggerFactory.getLogger(this.getClass)

  import StandardCharsets.UTF_8

  val dbPath = new File(MyConf.dbRootDir, "category/main")
  if (!dbPath.getParentFile.exists())
    dbPath.getParentFile.mkdirs()


  RocksDB.loadLibrary()

  val options = new DBOptions().setCreateIfMissing(!MyConf.categoryDbReadOnly)
    .setMaxBackgroundCompactions(10)
    .setCreateMissingColumnFamilies(!MyConf.categoryDbReadOnly)

  protected val cfNames = Lists.newArrayList[ColumnFamilyDescriptor](
    new ColumnFamilyDescriptor("default".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("name2id".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("inlinks".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("outlinks".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("pages".getBytes(UTF_8))
  )

  protected val cfHandlers = Lists.newArrayList[ColumnFamilyHandle]

  protected val db = RocksDB.open(options, dbPath.getAbsolutePath, cfNames, cfHandlers)

  protected val id2nameHandler: ColumnFamilyHandle = cfHandlers.get(0)
  protected val name2idHandler: ColumnFamilyHandle = cfHandlers.get(1)
  protected val inlinksHandler: ColumnFamilyHandle = cfHandlers.get(1)
  protected val outlinksHandler: ColumnFamilyHandle = cfHandlers.get(1)
  protected val pagesHandler: ColumnFamilyHandle = cfHandlers.get(1)

  def build() = {
    val pageSize = 5000
    val count = Await.result(CategoryRepo.count(), Duration.Inf)
    val pages = count / pageSize + 1
    (1 to pages) foreach {
      page =>
        println(s"process $page / $pages ...")
        val categories = Await.result(CategoryRepo.list(page, pageSize), Duration.Inf)

        categories.foreach {
          c =>
            saveIdName(c.id, c.name)
            saveInlinks(c.id)
            saveOutlinks(c.id)
            savePages(c.id)
        }
    }

    println("DONE")
  }

  /**
    * 获取当前分类的入链，出链，和页面数量之和
    */
  def getLinkedCount(cid: Int): Int = {
    getInlinkCount(cid).getOrElse(0) +
      getOutlinkCount(cid).getOrElse(0) +
      getPageCount(cid).getOrElse(0)
  }

  /**
    * 创建类别的ID和名称的双向映射，方便根据ID查名称，或者根据名称查ID
    */
  private def saveIdName(id: Int, name: String): Unit = {
    val idBytes = ByteUtil.int2bytes(id)
    val nameBytes = ByteUtil.string2bytes(name)
    db.put(id2nameHandler, idBytes, nameBytes)
    db.put(name2idHandler, nameBytes, idBytes)
  }

  def getNameById(id: Int): Option[String] = Option(
    db.get(id2nameHandler, ByteUtil.int2bytes(id))
  ).map(ByteUtil.bytes2string)

  def getIdByName(name: String): Option[Int] = Option(
    db.get(name2idHandler, ByteUtil.string2bytes(name))
  ).map(ByteUtil.bytes2Int)

  private def saveInlinks(cid: Int) = {
    val links = Await.result(CategoryInlinkRepo.findInlinksById(cid), Duration.Inf)
    val key = ByteUtil.int2bytes(cid)
    val value = getBytesFromSeq(links)
    db.put(inlinksHandler, key, value)
  }

  def getInlinks(cid: Int): Seq[Int] = Option(
    db.get(inlinksHandler, ByteUtil.int2bytes(cid))
  ) match {
    case Some(bytes) => readSeqFromBytes(bytes)
    case None => Seq.empty
  }

  def getInlinkCount(cid: Int): Option[Int] = Option(
    db.get(inlinksHandler, ByteUtil.int2bytes(cid))
  ).map(readSeqSizeFromBytes)

  private def saveOutlinks(cid: Int) = {
    val links = Await.result(CategoryOutlinkRepo.findOutlinksById(cid), Duration.Inf)
    val key = ByteUtil.int2bytes(cid)
    val value = getBytesFromSeq(links)
    db.put(outlinksHandler, key, value)
  }

  def getOutlinks(cid: Int): Seq[Int] = Option(
    db.get(outlinksHandler, ByteUtil.int2bytes(cid))
  ) match {
    case Some(bytes) => readSeqFromBytes(bytes)
    case None => Seq.empty
  }

  def getOutlinkCount(cid: Int): Option[Int] = Option(
    db.get(outlinksHandler, ByteUtil.int2bytes(cid))
  ).map(readSeqSizeFromBytes)

  private def savePages(cid: Int) = {
    val pageIds = Await.result(CategoryPageRepo.findPagesById(cid), Duration.Inf)
    val key = ByteUtil.int2bytes(cid)
    val value = getBytesFromSeq(pageIds)
    db.put(pagesHandler, key, value)
  }

  def getPages(cid: Int): Seq[Int] = Option(
    db.get(pagesHandler, ByteUtil.int2bytes(cid))
  ) match {
    case Some(bytes) => readSeqFromBytes(bytes)
    case None => Seq.empty
  }

  def getPageCount(cid: Int): Option[Int] = Option(
    db.get(pagesHandler, ByteUtil.int2bytes(cid))
  ).map(readSeqSizeFromBytes)

  private def getBytesFromSeq(ids: Seq[Int]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val dos = new DataOutputStream(out)

    dos.writeInt(ids.size)
    ids.foreach(dos.writeInt(_))

    dos.close()
    out.close()

    out.toByteArray
  }

  private def readSeqFromBytes(bytes: Array[Byte]): Seq[Int] = {
    val din = new DataInputStream(new ByteArrayInputStream(bytes))
    val count = din.readInt()
    val ids = (0 until count).map(_ => din.readInt()).toSeq
    din.close
    ids
  }

  private def readSeqSizeFromBytes(bytes: Array[Byte]): Int = {
    val din = new DataInputStream(new ByteArrayInputStream(bytes))
    val count = din.readInt()
    din.close
    count
  }

  /**
    * 数据库名字
    */
  def dbName: String = "Category DB"

  override def close(): Unit = {
    print(s"==> Close Category Db ... ")
    cfHandlers.forEach(h => h.close())
    db.close()
    println("DONE.")
  }
}