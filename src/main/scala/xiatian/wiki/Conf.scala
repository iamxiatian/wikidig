package xiatian.wiki

import java.io.File

import com.typesafe.config.ConfigFactory

/**
  * System Settings configuration
  *
  * @author Tian Xia
  *         June 04, 2017 13:20
  */
object Conf {
  val version = "1.0"

  //先采用my.conf中的配置，再使用application.conf中的默认配置
  lazy val config =
    ConfigFactory.parseFile(new java.io.File("./conf/my.conf"))
      .withFallback(ConfigFactory.load())

  def getString(path: String) = config.getString(path)

  def getInt(path: String) = config.getInt(path)

  def getBoolean(path: String) = config.getBoolean(path)

  lazy val mongoUrl = getString("db.mongo.url")

  val articleDbFile = new File("/opt/wiki/articles.db")

  def printConfig =
    s"""My configuration:
       |* database config:
       |    mongo url ==> ${mongoUrl}
    """.stripMargin
}