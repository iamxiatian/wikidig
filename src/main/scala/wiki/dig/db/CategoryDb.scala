package wiki.dig.db

import java.io.File
import java.nio.charset.StandardCharsets

import com.google.common.collect.Lists
import org.rocksdb._
import org.slf4j.LoggerFactory
import wiki.dig.db.ast.Db
import wiki.dig.repo.CategoryRepo
import wiki.dig.util.ByteUtil

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CategoryDb extends Db {
  val LOG = LoggerFactory.getLogger(this.getClass)

  import StandardCharsets.UTF_8

  val dbPath = new File("./db/category.kv.db")
  if (!dbPath.getParentFile.exists())
    dbPath.getParentFile.mkdirs()


  RocksDB.loadLibrary()

  val options = new DBOptions().setCreateIfMissing(true)
    .setMaxBackgroundCompactions(10)
    .setCreateMissingColumnFamilies(true)

  protected val cfNames = Lists.newArrayList[ColumnFamilyDescriptor](
    new ColumnFamilyDescriptor("id2name".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("name2id".getBytes(UTF_8)) //元数据族
  )

  protected val cfHandlers = Lists.newArrayList[ColumnFamilyHandle]

  protected val db = RocksDB.open(options, dbPath.getAbsolutePath, cfNames, cfHandlers)

  protected val id2nameHandler: ColumnFamilyHandle = cfHandlers.get(0)
  protected val name2idHandler: ColumnFamilyHandle = cfHandlers.get(1)

  /**
    * 创建类别的ID和名称的双向映射，方便根据ID查名称，或者根据名称查ID
    */
  def save(id: Int, name: String): Unit = {
    val idBytes = ByteUtil.int2bytes(id)
    val nameBytes = ByteUtil.string2bytes(name)
    db.put(id2nameHandler, idBytes, nameBytes)
    db.put(name2idHandler, nameBytes, idBytes)
  }

  /**
    * 根据类别的id获得类别的名称
    *
    * @param id
    * @return
    */
  def getNameById(id: Int): Option[String] = {
    val idBytes = ByteUtil.int2bytes(id)

    Option(db.get(id2nameHandler, idBytes)).map(ByteUtil.bytes2string)
  }

  /**
    * 根据类别的名称获得类别的ID
    *
    * @param name
    * @return
    */
  def getIdByName(name: String): Option[Int] = {
    val nameBytes = ByteUtil.string2bytes(name)
    Option(db.get(name2idHandler, nameBytes)).map(ByteUtil.bytes2Int)
  }

  def build() = {
    val pageSize = 10000
    val count = Await.result(CategoryRepo.count(), Duration.Inf)
    val pages = count / pageSize + 1
    (1 to pages) foreach {
      page =>
        println(s"process $page / $pages ...")
        val categories = Await.result(CategoryRepo.list(page, pageSize), Duration.Inf)

        categories.foreach(c => save(c.id, c.name))
    }

    println("DONE")
  }

  /**
    * 数据库名字
    */
  def dbName: String = "Category DB"

  override def close(): Unit = {
    print(s"==> Close Category Db ... ")
    id2nameHandler.close()
    name2idHandler.close()
    db.close()
    println("DONE.")
  }
}