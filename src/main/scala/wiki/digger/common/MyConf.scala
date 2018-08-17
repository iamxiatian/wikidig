package wiki.digger.common

import java.io.File
import java.io.File
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.config.impl.Parseable
import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions}

import scala.collection.JavaConverters._
import scala.io.Source
/**
  * System Settings configuration
  *
  * @author Tian Xia
  *         June 04, 2017 13:20
  */
object MyConf {
  val version = BuildInfo.version

  /** AkkaSystem使用的配置类，其启动时需要指定该类 */
  var akkaMasterConfig: Option[Config] = None

  //先采用my.conf中的配置，再使用application.conf中的默认配置
  lazy val config: Config = {
    val confFile = new File("./conf/my.conf")

    //先采用conf/my.conf中的配置，再使用application.conf中的默认配置
    if (confFile.exists()) {
      println(s"启用配置文件${confFile.getCanonicalPath}")
      val unresolvedResources = Parseable
        .newResources("application.conf", ConfigParseOptions.defaults())
        .parse()
        .toConfig()

      ConfigFactory.parseFile(confFile).withFallback(unresolvedResources).resolve()
    } else {
      ConfigFactory.load()
    }
  }

  def getString(path: String) = config.getString(path)

  def getInt(path: String) = config.getInt(path)

  def getBoolean(path: String) = config.getBoolean(path)


  val screenConfigText: String = {
    s"""
       |My configuration(build: ${BuildInfo.builtAtString}):
       |├── master config:
       |│   ├── hostname ==> xxxx
       |│   ├── port ==> 7000
       |│   └── API http port ==> ssss
       |│
       |└── fetcher config:
       |    ├── fetcher identification ==> xxx
       |    └── API http port ==> ssss
       |"""
  }
}