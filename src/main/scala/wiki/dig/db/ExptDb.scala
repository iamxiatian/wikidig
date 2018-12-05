package wiki.dig.db

import java.io._
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat

import breeze.linalg._
import com.google.common.collect.Lists
import com.google.common.io.Files
import org.apache.commons.lang3.StringUtils
import org.rocksdb._
import org.slf4j.LoggerFactory
import wiki.dig.common.MyConf
import wiki.dig.db.ast.{Db, DbHelper}
import wiki.dig.util.ByteUtil

import scala.io.Source

/**
  * 试验分析用的临时数据库，后续会删除
  *
  */
object ExptDb extends Db with DbHelper {
  val LOG = LoggerFactory.getLogger(this.getClass)

  import StandardCharsets.UTF_8

  val dbPath = new File(MyConf.dbRootDir, "expt")
  if (!dbPath.getParentFile.exists())
    dbPath.getParentFile.mkdirs()

  RocksDB.loadLibrary()

  val options = new DBOptions().setCreateIfMissing(true)
    .setMaxBackgroundCompactions(10)
    .setCreateMissingColumnFamilies(true)

  protected val cfNames = Lists.newArrayList[ColumnFamilyDescriptor](
    new ColumnFamilyDescriptor("default".getBytes(UTF_8)), //默认为GloVE 100
    new ColumnFamilyDescriptor("glove50".getBytes(UTF_8))
  )

  protected val cfHandlers = Lists.newArrayList[ColumnFamilyHandle]

  protected val db = RocksDB.open(options, dbPath.getAbsolutePath, cfNames, cfHandlers)

  protected val defaultHandler: ColumnFamilyHandle = cfHandlers.get(0)
  protected val glove50Handler: ColumnFamilyHandle = cfHandlers.get(1)

  def buildArticleEmbedding(articleIdFile: File = new File("./sample.page.ids.txt")) = {
    val pageEmbeddingWriter = Files.newWriter(new File("sample.page.embedding.txt"), UTF_8)
    val source = Source.fromFile(articleIdFile, "UTF-8")

    val f = new DecimalFormat("0.#####")
    source.getLines().filter(line => line.nonEmpty && !line.startsWith("#"))
      .zipWithIndex
      .foreach {
        case (line, idx) =>
          val pid = line.toInt
          PageContentDb.getContent(pid).map {
            text =>
              val v = calculateVector(text)
              db.put(ByteUtil.int2bytes(pid), getBytesFromFloatSeq(v.data))

              val stringValues = v.data.map(f.format(_)).mkString(" ")
              pageEmbeddingWriter.write(s"$pid ${stringValues}")
          }
          if (idx % 500 == 0) {
            println(idx)
            pageEmbeddingWriter.flush()
          }
      }
    source.close()
    pageEmbeddingWriter.close()
    println("DONE")
  }

  def calculateVector(text: String): DenseVector[Float] = {
    //分词，并转换为词语的embedding表示的vector列表
    val vectors: Seq[DenseVector[Float]] = StringUtils.split(text, " \t\n\r\"").map {
      token =>
        if ((token.endsWith(".") && token.indexOf(".") == token.length - 1) ||
          token.endsWith(",") ||
          token.endsWith("!") ||
          token.endsWith("?")
        )
          token.substring(0, token.length - 1)
        else
          token
    }.flatMap {
      token =>
        EmbeddingDb.find(token.toLowerCase).map {
          v => DenseVector(v.toArray)
        }
    }

    //取平均值作为文本的embedding结果
    val total = DenseVector.zeros[Float](vectors(0).length)
    vectors.foreach {
      v =>
        total += v
    }

    val count: Float = vectors.size
    if (count > 0) total /= count else total
  }

  def find(id: Int): Option[Seq[Float]] =
    Option(db.get(defaultHandler, ByteUtil.int2bytes(id))).map {
      readFloatSeqFromBytes(_)
    }

  /**
    * 数据库名字
    */
  def dbName: String = "Expt DB"

  override def close(): Unit = {
    print(s"==> Close $dbName ... ")
    cfHandlers.forEach(h => h.close())
    db.close()
    println("DONE.")
  }
}