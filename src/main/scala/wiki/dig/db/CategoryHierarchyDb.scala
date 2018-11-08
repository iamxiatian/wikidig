package wiki.dig.db

import java.io._
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

import com.google.common.collect.Lists
import org.apache.commons.lang3.StringUtils
import org.rocksdb._
import org.slf4j.LoggerFactory
import wiki.dig.common.MyConf
import wiki.dig.db.ast.Db
import wiki.dig.repo.CategoryRepo
import wiki.dig.util.ByteUtil

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * wiki层级体系数据库，记录了层级之间的父子关系. 层级体系数据库依赖于CategoryDb主数据库
  * 事先构建成功。(see CategoryDb.build)
  */
object CategoryHierarchyDb extends Db {
  val LOG = LoggerFactory.getLogger(this.getClass)

  import StandardCharsets.UTF_8

  val dbPath = new File(MyConf.dbRootDir, "category/hierarchy")
  if (!dbPath.getParentFile.exists())
    dbPath.getParentFile.mkdirs()

  RocksDB.loadLibrary()

  val options = new DBOptions().setCreateIfMissing(true)
    .setMaxBackgroundCompactions(10)
    .setCreateMissingColumnFamilies(true)

  protected val cfNames = Lists.newArrayList[ColumnFamilyDescriptor](
    new ColumnFamilyDescriptor("default".getBytes(UTF_8)),
    new ColumnFamilyDescriptor("meta".getBytes(UTF_8)) //元数据族
  )

  protected val cfHandlers = Lists.newArrayList[ColumnFamilyHandle]

  protected val db = RocksDB.open(options, dbPath.getAbsolutePath, cfNames, cfHandlers)

  protected val defaultHandler: ColumnFamilyHandle = cfHandlers.get(0)
  protected val metaHandler: ColumnFamilyHandle = cfHandlers.get(1)

  val Max_Depth = 5


  def accept(name: String): Boolean = {
    val title = name.replaceAll("_", " ").toLowerCase()

    if (title.length > 7) { //保留1980s此类词条
      val startString = title.substring(0, 4)
      if (StringUtils.isNumeric(startString)) return false
    }

    //step 2: remove "list of xxxx" and "index of xxx"
    if (title.indexOf("index of ") >= 0 ||
      title.indexOf("list of") >= 0 ||
      title.indexOf("(disambiguation)") >= 0) return false

    //以年份结尾的词条，符合年份时代结尾的形式文章，如``China national football team results (2000–09)''，因为这类文章的作用更类似于类别，起到信息组织的作用。
    val pattern = Pattern.compile("\\(\\d{4}(–|\\-)\\d{2,4}\\)$")
    if (pattern.matcher(title).find) return false

    return true
  }

  def build() = {
    val startNodes = Await.result(CategoryRepo.levelOne(), Duration.Inf).map(_.id)
    val queue = mutable.Queue.empty[(Int, Int)]

    var totalWeight: Long = 0L

    var counter = 0

    startNodes.foreach(id => queue.enqueue((id, 1)))

    startNodes.foreach(println)

    val sb = new java.lang.StringBuilder()
    while (queue.nonEmpty) {
      val (cid, depth) = queue.dequeue()

      val key = ByteUtil.int2bytes(cid)

      if (getCNode(cid).isEmpty) {
        counter += 1
        if (counter % 1000 == 0) {
          println(s"processing $counter, queue size: ${queue.size}")
        }
        //之前没有保存过，已保证保存的depth最小。
        val outlinks = CategoryDb.getOutlinks(cid).filter {
          id =>
            CategoryDb.getNameById(id) match {
              case Some(name) =>
                accept(name)
              case None => false
            }
        }
        val weights = outlinks.map(CategoryDb.getLinkedCount(_))

        val weight = CategoryDb.getLinkedCount(cid)
        totalWeight += weight

        val node = CNode(depth, weight, outlinks, weights)
        db.put(key, node.toBytes())
        if (depth <= Max_Depth) {
          outlinks.foreach(id => queue.enqueue((id, depth + 1)))
        }
      } else {
        //println(s"$cid / $depth")
        println(".")
      }
    }

    db.put(metaHandler, "TotalWeight".getBytes(UTF_8), ByteUtil.long2bytes(totalWeight))
    println("DONE")
  }

  def getCNode(cid: Int): Option[CNode] = Option(
    db.get(ByteUtil.int2bytes(cid))
  ) map readCNode

  def getTotalWeight(): Long = Option(
    db.get(metaHandler, "TotalWeight".getBytes(UTF_8))
  ).map(ByteUtil.bytes2Long(_)).getOrElse(1)

  /**
    * 数据库名字
    */
  def dbName: String = "Category Hierarchy DB"

  override def close(): Unit = {
    print(s"==> Close Category Hierarchy Db ... ")
    cfHandlers.forEach(h => h.close())
    db.close()
    println("DONE.")
  }

  def readCNode(bytes: Array[Byte]): CNode = {
    val din = new DataInputStream(new ByteArrayInputStream(bytes))
    val depth = din.readInt()
    val weight = din.readInt()
    val count = din.readInt()
    val outlinks = (0 until count).map(_ => din.readInt())
    val weights = (0 until count).map(_ => din.readInt())
    din.close
    CNode(depth, weight, outlinks, weights)
  }
}

/**
  * 类别节点
  *
  * @param depth    当前类别节点的深度
  * @param weight   当前类别的权重
  * @param outlinks 当前类别节点的出链，即下级子类
  * @param weights  对应子类的权重，当前为出入量数量+页面数量
  */
case class CNode(depth: Int,
                 weight: Int,
                 outlinks: Seq[Int],
                 weights: Seq[Int]
                ) {
  def toBytes(): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val dos = new DataOutputStream(out)

    dos.writeInt(depth)
    dos.writeInt(weight)

    dos.writeInt(outlinks.size)
    outlinks.foreach(dos.writeInt(_))
    weights.foreach(dos.writeInt(_))
    dos.close()
    out.close()

    out.toByteArray
  }
}