package wiki.dig

import wiki.dig.common.{BuildInfo, MyConf}
import wiki.dig.db.CategoryDb

/**
  * Application Start
  */
object Start extends App {

  case class Config(
                     buildCategoryPairs: Boolean = false
                   )

  val parser = new scopt.OptionParser[Config]("bin/spider") {
    head(s"${BuildInfo.name}", s"${BuildInfo.version}")

    import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

    val format: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    opt[Unit]("buildCategoryPairs").action((_, c) =>
      c.copy(buildCategoryPairs = true)).text("build category id - name pairs to rocksdb.")

    help("help").text("prints this usage text")

    note("\n xiatian, xia(at)ruc.edu.cn.")
  }

  println(MyConf.screenConfigText)

  parser.parse(args, Config()) match {
    case Some(config) =>
      if (config.buildCategoryPairs) {
        CategoryDb.build()
        CategoryDb.close()
      }

    case None => {
      println( """Wrong parameters :(""".stripMargin)
    }
  }

}
