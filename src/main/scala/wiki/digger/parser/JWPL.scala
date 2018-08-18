package wiki.digger.parser

import wiki.digger.common.MyConf

/**
  * JWPL tools(https://dkpro.github.io/dkpro-jwpl/DataMachine/)
  */
object JWPL {

  import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration
  import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language

  val dbConfig = new DatabaseConfiguration
  dbConfig.setHost(MyConf.)
  dbConfig.setDatabase("DATABASE")
  dbConfig.setUser("USER")
  dbConfig.setPassword("PASSWORD")
  dbConfig.setLanguage(Language.german)
}
