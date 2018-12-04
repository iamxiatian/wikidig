package wiki.dig

import wiki.dig.common.{BuildInfo, MyConf}
import wiki.dig.db.{CategoryDb, CategoryHierarchyDb, PageContentDb, PageDb}

/**
  * Application Start
  */
object Start extends App {

  case class Config(
                     buildCategoryPairs: Boolean = false,
                     buildHierarchy: Boolean = false,
                     buildPageDb: Boolean = false,
                     buildPageContentDb: Boolean = false,
                     sample: Option[Int] = None,
                     startPage: Int = 1,
                     pageSize: Int = 500
                   )

  val parser = new scopt.OptionParser[Config]("bin/spider") {
    head(s"${BuildInfo.name}", s"${BuildInfo.version}")

    import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

    val format: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    opt[Unit]("buildCategoryPairs").action((_, c) =>
      c.copy(buildCategoryPairs = true)).text("build category info to rocksdb.")

    opt[Unit]("buildHierarchy").action((_, c) =>
      c.copy(buildHierarchy = true)).text("build category Hierarchy to rocksdb.")

    opt[Unit]("buildPageDb").action((_, c) =>
      c.copy(buildPageDb = true)).text("build page db with rocksdb format.")

    opt[Unit]("buildPageContentDb").action((_, c) =>
      c.copy(buildPageDb = true)).text("build page content db with rocksdb format.")

    opt[Int]('s', "sample").optional().
      action((x, c) => c.copy(sample = Some(x))).
      text("sample n triangles.")

    opt[Int]("startPage").action(
      (x, c) =>
        c.copy(startPage = x)
    ).text("build db from specified page.")

    opt[Int]("pageSize").action(
      (x, c) =>
        c.copy(pageSize = x)
    ).text("page size when build db.")

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

      if (config.buildHierarchy) {
        CategoryHierarchyDb.build()
        CategoryHierarchyDb.close()
      }

      if (config.sample.nonEmpty) {
        val n = config.sample.get
        CategoryHierarchyDb.sample(n)
      }

      if (config.buildPageDb) {
        println("build page db ...")
        PageDb.build(config.startPage, config.pageSize)
        PageDb.close()
      }

      if (config.buildPageContentDb) {
        println("build page db ...")
        PageContentDb.build(config.startPage, config.pageSize)
        PageContentDb.close()
      }
    case None => {
      println( """Wrong parameters :(""".stripMargin)
    }
  }

}
