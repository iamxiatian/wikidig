package wiki.digger.dump

import java.io.{DataOutputStream, IOException}
import java.util
import java.util.Set
import java.util.regex.{Matcher, Pattern}

import com.google.common.base.Strings
import xiatian.wiki.model.{Link, WikiType}


/**
  *
  * @param id
  * @param title
  * @param ns
  * @param text
  * @param format
  * @param redirect
  * @param commonsCatTag
  * @param commonCategory
  * @param inlinkCount 入链数量，默认为-1，表示没有该信息，如大于等于0，表示为统计出的真实数据.
  *                    对于文章，该数值为入链，对于分类，该数值为拥有的文章数量
  */
case class WikiPage(id: Int,
                    title: String,
                    `type`: WikiType.Value,
                    ns: String,
                    text: String,
                    format: String,
                    redirect: String,
                    commonsCatTag: String,
                    commonCategory: Boolean,
                    inlinkCount: Int = -1,
                    plainText: String,
                    categories: Set[String],
                    internalLinks: Seq[String],
                    externalLinks: Seq[Link]
                   )

object WikiPage {

  /**
    *  Guess wiki page type by its namespace, format and text.
    */
  def guessType(ns: String, format: String, text: String):WikiType.Value =
    if (ns == "14")
      WikiType.Category
    else if (
      !Strings.isNullOrEmpty(text)
        && (Strings.isNullOrEmpty(ns) || ns == "0")
        && "text/x-wiki" == format
    ) {
      WikiType.Article
    } else {
      WikiType.Other
    }

//
//  def drillMoreInfo(): Unit = {
//    internalLinks = new util.ArrayList[String]
//    categories = new util.HashSet[String]
//    if (!isArticle && !isCategory) return
//    //parse category
//    if (isCategory) {
//      commonCategory = true
//      //if it's root category, return as normal category
//      if (!conf.getWikiRootCategoryName.equalsIgnoreCase(getCategoryTitle)) { //解析所隶属的类别
//        categories = WikiTextParser.parseCategories(text)
//        if (CollectionUtils.isEmpty(categories)) {
//          commonCategory = false
//          //judge is a category redirect or not
//          this.redirect = WikiTextParser.parseCategoryRedirect(text)
//          if (!isRedirect) this.commonsCatTag = WikiTextParser.parseCommonsCat(text)
//        }
//      }
//      return
//    }
//    categories = WikiTextParser.parseCategories(text)
//    val pf: MediaWikiParserFactory = new MediaWikiParserFactory
//    val parser: MediaWikiParser = pf.createParser
//    val pp: ParsedPage = parser.parse(text)
//    if (pp == null) {
//      plainText = ""
//      System.out.println("text parse error: id==>" + id + ", title==>" + title + ", ns==>" + ns + ", content==>" + text)
//      return
//    }
//    plainText = ""
//    for (s <- pp.getSections) {
//      if (s.getTitle != null) plainText += s.getTitle + "\n"
//      for (p <- s.getParagraphs) {
//        val par: String = p.getText
//        if (par.startsWith("TEMPLATE")) continue //todo: continue is not supported
//        if (par.matches("[^:]+:[^\\ ]+")) continue //todo: continue is not supported
//        plainText += par + "\n\n"
//      }
//    }
//    for (link <- pp.getLinks) {
//      if (link.getType eq Link.`type`.INTERNAL) internalLinks.add(link.getTarget)
//      pageLinks.add(link)
//    }
//  }
//
//  def getPlainText: String = {
//    if (plainText == null) {
//      val sb: StringBuilder = new StringBuilder
//      val pf: MediaWikiParserFactory = new MediaWikiParserFactory
//      val parser: MediaWikiParser = pf.createParser
//      val pp: ParsedPage = parser.parse(text)
//      if (pp != null) {
//        for (s <- pp.getSections) {
//          if (s.getTitle != null) {
//            sb.append(s.getTitle).append("\n")
//            //plainText += s.getTitle() + "\n";
//          }
//          for (p <- s.getParagraphs) {
//            val par: String = p.getText
//            if (par.startsWith("TEMPLATE")) continue //todo: continue is not supported
//            if (par.matches("[^:]+:[^\\ ]+")) continue //todo: continue is not supported
//            //处理内容里面是否包含TEMPLATE
//            val pattern: Pattern = Pattern.compile("TEMPLATE\\[[^\\]]+\\]", Pattern.CASE_INSENSITIVE)
//            val matcher: Matcher = pattern.matcher(par)
//            var last: Int = 0
//            while ( {
//              matcher.find(last)
//            }) {
//              sb.append(par.substring(last, matcher.start))
//              last = matcher.end
//            }
//            sb.append(par.substring(last))
//            sb.append("\n\n")
//            //plainText += par + "\n\n";
//            //sb.append(par).append("\n\n");
//          }
//        }
//      }
//      plainText = sb.toString
//    }
//    plainText
//  }
//
//  def getCategories: util.Set[String] = categories
//
//  def getAliases: util.Set[String] = aliases
//
//  def setAliases(aliases: util.Set[String]): Unit = {
//    this.aliases = aliases
//  }
//
//  def getInternalLinks: util.List[String] = this.internalLinks
//
//  def setInternalLinks(internalLinks: util.List[String]): Unit = {
//    this.internalLinks = internalLinks
//  }
//
//  def getCategoryTitle: String = if (this.title.toLowerCase.startsWith("category:")) this.title.substring("Category:".length)
//  else if (this.title == "页面分类:") this.title.substring("页面分类:".length)
//  else title
//
//  @throws[IOException]
//  def writeIn(dos: DataOutputStream): Unit = { //write page type: 0 for article, 1 for category
//    if (isArticle) dos.writeByte(0)
//    else if (isCommonCategory) { //common category
//      dos.writeByte(1)
//    }
//    else if (isCommonsCatTag) { //not command category
//      dos.writeByte(2)
//    }
//    else throw new IOException("The page can not be written to file:" + toString)
//    dos.writeInt(getId)
//    dos.writeUTF(if (title == null) ""
//    else title)
//    dos.writeUTF(if (redirect == null) ""
//    else redirect)
//    dos.writeUTF(if (commonsCatTag == null) ""
//    else commonsCatTag)
//    val textBuffer: Array[Byte] = text.getBytes("utf-8")
//    dos.writeInt(textBuffer.length)
//    dos.write(textBuffer, 0, textBuffer.length)
//    dos.writeInt(categories.size)
//    for (c <- categories) {
//      dos.writeUTF(c)
//    }
//    dos.writeInt(getAliases.size)
//    import scala.collection.JavaConversions._
//    for (alias <- getAliases) {
//      dos.writeUTF(alias)
//    }
//    dos.writeInt(internalLinks.size)
//    for (link <- internalLinks) {
//      dos.writeUTF(link)
//    }
//    dos.writeInt(inlinkCount)
//    dos.flush()
//  }
}
