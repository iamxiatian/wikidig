package xiatian.wiki.db

import java.io.File
import java.nio.charset.StandardCharsets

import org.rocksdb._
import org.rocksdb.util.SizeUnit
import org.slf4j.LoggerFactory

/**
  *
  * @author Tian Xia
  *         School of IRM, Renmin University of China.
  *         Oct 10, 2017 16:29
  */
class StoreRocksDB(dbFile: File, readOnly: Boolean = false) {
  val log = LoggerFactory.getLogger(this.getClass)
  val UTF8 = StandardCharsets.UTF_8

  RocksDB.loadLibrary()

  val options = new Options().setCreateIfMissing(!readOnly)
    .setWriteBufferSize(200 * SizeUnit.MB)
    .setMaxWriteBufferNumber(3)
    .setMaxBackgroundCompactions(10)
    .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
    .setCompactionStyle(CompactionStyle.UNIVERSAL)


  // a factory method that returns a RocksDB instance
  val db: RocksDB = {
    val rocksDB = RocksDB.open(options, dbFile.getAbsolutePath)
    log.debug(s"Open RocksDB from ${dbFile.getCanonicalFile}")
    rocksDB
  }

  def put(k: String, v: String) =
    db.put(
      k.getBytes(UTF8),
      v.getBytes(UTF8)
    )

  def commit = {}

  // if l is sorted by key is better
  def putList(l: List[(String, String)]) = {
    val writeOpt = new WriteOptions()
    val batch = new WriteBatch()
    l.foreach(ll => {
      val (k, v) = ll
      batch.put(k.getBytes(UTF8), v.getBytes(UTF8))
    })
    db.write(writeOpt, batch)
  }


  def get(k: String): Option[String] = {
    try {
      val v = db.get(k.getBytes(UTF8))
      Some(new String(v, UTF8))
    } catch {
      case e: java.lang.NullPointerException => None
    }
  }

  def shutdown() = db.close()
}
