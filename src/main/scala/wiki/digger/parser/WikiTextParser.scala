package wiki.digger.parser

import java.util
import java.util.List
import java.util.regex.Pattern

object WikiTextParser {
  private val CATEGORY_PATTERN = Pattern.compile("\\[\\[Category\\:([^\\]|^\\|]+)(\\|[^\\]]+)?\\]\\]", Pattern.CASE_INSENSITIVE)

  /**
    * 对带有格式的wiki文本，抽取其中的类别信息
    * @param formatSource
    * @return
    */
  def parseCategories(formatSource: String): List[String] = {
    val categories = new util.HashSet[String]
    val matcher = CATEGORY_PATTERN.matcher(formatSource)

    while ( {
      matcher.find
    }) categories.add(matcher.group(1))
    //skip numbers
    categories
  }

  private val CATEGORY_REDIRECT_PATTERN = Pattern.compile("\\{\\{(分类重定向|cr)\\|([^\\}]+)\\}\\}", Pattern.CASE_INSENSITIVE)

  /**
    * 重定向语法：
    * {{cr|基督教新教}}
    * {{分类重定向|历史学书籍}}
    *
    * @param source
    * @return
    */
  def parseCategoryRedirect(source: String): String = {
    val matcher = CATEGORY_REDIRECT_PATTERN.matcher(source)
    while ( {
      matcher.find
    }) return matcher.group(2)
    null
  }

  def parseInternalLinks(text: String): util.List[String] = {
    val pf = new MediaWikiParserFactory
    val parser = pf.createParser
    val pp = parser.parse(text)
    val internalLinks = new util.LinkedList[String]
    if (pp != null) {
      for (link <- pp.getLinks) {
        if (link.getType eq Link.`type`.INTERNAL) internalLinks.add(link.getTarget)
      }
    }
    internalLinks
  }

  private val COMMONS_CATEGORY_PATTERN = Pattern.compile("\\{\\{Commonscat\\|([^\\}]+)\\}\\}", Pattern.CASE_INSENSITIVE)

  /**
    * 在维基百科条目中引用其姊妹计划——维基共享资源的分类:
    * {{Catnav|页面分类|人物|出生|20世纪出生|1930年代出生}}\n\n{{出生年|193|3}}\n{{Commonscat|1933 births}}
    */
  def parseCommonsCat(source: String): String = {
    val matcher = COMMONS_CATEGORY_PATTERN.matcher(source)
    while ( {
      matcher.find
    }) return matcher.group(1)
    null
  }

  def main(args: Array[String]): Unit = {
    val array = Array[String]("{{filmyr|200|4}}\n{{Commonscat|2004 in film}}", "sfdsfajsd[[category:中国]]hsdlf[[Category:中国2|sd]]jsdl", "{{Catnav|页面分类|人物|出生|20世纪出生|1930年代出生}}\\n\\n{{出生年|193|3}}\\n{{Commonscat|1933 births}}", "{{分类重定向|历史学书籍}}", "1978年{{分类重定向|历史学书籍}}ss", "{{cr|基督教新教}}%", "0.25", "中国")
    for (s <- array) { //            System.out.println(s + "==>" + parseCategories(s));
      //            System.out.println(s + "==>" + parseCategoryRedirect(s));
      System.out.println(s + "==>" + parseCommonsCat(s))
    }
  }
}
