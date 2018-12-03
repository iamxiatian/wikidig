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
  * 把Page的信息保存到RocksDB数据库中，里面记录的信息包括：
  *
  * Page的id:name双向映射关系，来自于数据库的Page. 由于有别名的存在，一个ID会对应到一个
  * 标准的词条名称上，但是有多个名称（规范的和非规范的）映射到一个id上。
  *
  * 页面的入链，来自于page_inlinks
  *
  * 页面的出链，来自于page_outlinks
  *
  * 页面指向的类别，来自于page_categories
  *
  * Page Redirects，来自于表：page_redirects
  */
object PageDb extends Db {
  val LOG = LoggerFactory.getLogger(this.getClass)

  import StandardCharsets.UTF_8

  val dbPath = new File(MyConf.dbRootDir, "page/main")
  if (!dbPath.getParentFile.exists())
    dbPath.getParentFile.mkdirs()


  RocksDB.loadLibrary()

  val options = new DBOptions().setCreateIfMissing(!MyConf.pageDbReadOnly)
    .setMaxBackgroundCompactions(10)
    .setCreateMissingColumnFamilies(!MyConf.pageDbReadOnly)

  protected val cfNames = Lists.newArrayList[ColumnFamilyDescriptor](
    new ColumnFamilyDescriptor("default".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("name2id".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("disambiguation".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("inlinks".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("outlinks".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("category".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("content".getBytes(UTF_8))
  )

  protected val cfHandlers = Lists.newArrayList[ColumnFamilyHandle]

  protected val db = RocksDB.open(options, dbPath.getAbsolutePath, cfNames, cfHandlers)

  protected val id2nameHandler: ColumnFamilyHandle = cfHandlers.get(0)
  protected val name2idHandler: ColumnFamilyHandle = cfHandlers.get(1)
  protected val disambiHandler: ColumnFamilyHandle = cfHandlers.get(2)
  protected val inlinksHandler: ColumnFamilyHandle = cfHandlers.get(3)
  protected val outlinksHandler: ColumnFamilyHandle = cfHandlers.get(4)
  protected val categoryHandler: ColumnFamilyHandle = cfHandlers.get(5)
  protected val contentHandler: ColumnFamilyHandle = cfHandlers.get(6)


  def build() = {
    val pageSize = 5000
    val count = Await.result(PageRepo.count(), Duration.Inf)
    val pageNum = count / pageSize + 1
    (1 to pageNum) foreach {
      p =>
        println(s"process $p / $pageNum ...")
        val pages = Await.result(PageRepo.list(p, pageSize), Duration.Inf)

        pages.foreach {
          page =>
            saveIdName(page.id, page.name)
            saveInlinks(page.id)
            saveOutlinks(page.id)

            //只记录是消歧义的页面，其他情况默认为非歧义页面
            if (page.isDisambiguation) {
              saveDisambiguation(page.id)
            }

            saveCategories(page.id)
            saveContent(page.id, page.text)
        }
    }

    println("DONE")
  }

  /**
    * 获取当前页面的入链和出链之和
    */
  def getLinkedCount(cid: Int): Int = {
    getInlinkCount(cid).getOrElse(0) + getOutlinkCount(cid).getOrElse(0)
  }

  /**
    * 创建ID和名称的双向映射，方便根据ID查名称，或者根据名称查ID
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
    val links = Await.result(PageInlinkRepo.findInlinksById(cid), Duration.Inf)
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

  def getInlinkCount(id: Int): Option[Int] = Option(
    db.get(inlinksHandler, ByteUtil.int2bytes(id))
  ).map(readSeqSizeFromBytes)

  private def saveOutlinks(id: Int) = {
    val links = Await.result(PageOutlinkRepo.findOutlinksById(id), Duration.Inf)
    val key = ByteUtil.int2bytes(id)
    val value = getBytesFromSeq(links)
    db.put(outlinksHandler, key, value)
  }

  def getOutlinks(id: Int): Seq[Int] = Option(
    db.get(outlinksHandler, ByteUtil.int2bytes(id))
  ) match {
    case Some(bytes) => readSeqFromBytes(bytes)
    case None => Seq.empty
  }

  def getOutlinkCount(id: Int): Option[Int] = Option(
    db.get(outlinksHandler, ByteUtil.int2bytes(id))
  ).map(readSeqSizeFromBytes)

  private def saveCategories(id: Int) = {
    val ids = Await.result(PageCategoryRepo.findCategoriesById(id), Duration.Inf)
    val key = ByteUtil.int2bytes(id)
    val value = getBytesFromSeq(ids)
    db.put(categoryHandler, key, value)
  }

  def getCategories(id: Int): Seq[Int] = Option(
    db.get(categoryHandler, ByteUtil.int2bytes(id))
  ) match {
    case Some(bytes) => readSeqFromBytes(bytes)
    case None => Seq.empty
  }

  def getCategoryCount(id: Int): Option[Int] = Option(
    db.get(categoryHandler, ByteUtil.int2bytes(id))
  ).map(readSeqSizeFromBytes)


  /**
    * 记录id为消歧义页面
    *
    * @param id
    */
  private def saveDisambiguation(id: Int): Unit = {
    val key = ByteUtil.int2bytes(id)
    val value: Array[Byte] = Array(1)
    db.put(disambiHandler, key, value)
  }

  /**
    * 判断id是否为消歧义页面
    *
    * @param id
    * @return
    */
  def isDisambiguation(id: Int): Boolean = {
    val key = ByteUtil.int2bytes(id)
    db.get(disambiHandler, key) != null
  }

  private def saveContent(id: Int, content: String) = {
    val key = ByteUtil.int2bytes(id)
    val value = GZipUtils.compress(content.getBytes(UTF_8))
    db.put(contentHandler, key, value)
  }

  def getContent(id: Int): Option[String] = Option(
    db.get(contentHandler, ByteUtil.int2bytes(id))
  ) match {
    case Some(bytes) =>
      val unzipped = GZipUtils.decompress(bytes)
      Option(new String(unzipped, UTF_8))

    case None => None
  }

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
    din.close()
    ids
  }

  private def readSeqSizeFromBytes(bytes: Array[Byte]): Int = {
    val din = new DataInputStream(new ByteArrayInputStream(bytes))
    val count = din.readInt()
    din.close()
    count
  }

  /**
    * 数据库名字
    */
  def dbName: String = "Page DB"

  override def close(): Unit = {
    print(s"==> Close Page Db ... ")
    cfHandlers.forEach(h => h.close())
    db.close()
    println("DONE.")
  }
}