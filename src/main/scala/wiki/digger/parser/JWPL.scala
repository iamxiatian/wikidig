package wiki.digger.parser

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language
import de.tudarmstadt.ukp.wikipedia.api.{DatabaseConfiguration, Wikipedia}
import wiki.digger.common.MyConf
/**
  * JWPL tools(https://dkpro.github.io/dkpro-jwpl/DataMachine/)
  */
object JWPL {

  val dbConfig = {
    val c = new DatabaseConfiguration
    c.setHost(MyConf.wikiDbHost)
    c.setDatabase(MyConf.wikiDbName)
    c.setUser(MyConf.wikiDbUser)
    c.setPassword(MyConf.wikiDbPassword)
    c.setLanguage(Language.english)
    c
  }

  // Create a new English wikipedia.
  val wiki = new Wikipedia(dbConfig)

  wiki.existsPage(1)
  wiki.getArticles
  wiki.getPageIds
  wiki.getPage(9)
  wiki.getPage("hello")

val p = wiki.getPage(0).getPlainText
  def main(args: Array[String]): Unit = {
    println(wiki.existsPage(1))
  }
}
