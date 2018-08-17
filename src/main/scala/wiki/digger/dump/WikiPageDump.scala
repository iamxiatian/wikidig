package wiki.digger.dump

import java.io.Closeable
import java.util

import org.slf4j.LoggerFactory

import scala.util.Try

/**
  * Wiki Page dump processor, there are 2 ways to visit the dump content:
  *
  * The first one is accessed by traverse as follows:
  * <pre>
  * WikiPageDump dump = new OneWikiPageDumpImpl();
  * dump.traverse(filters);
  * </pre>
  *
  * The second one is used by iterator:
  * <pre>
  * WikiPageDump dump = new OneWikiPageDumpImpl();
  *     dump.open();
  * while(dump.hasNext()){
  * WikiPage page = dump.next();
  * //do some process
  * }
  *     dump.close();
  * </pre>
  *
  * @author Tian Xia
  */
abstract class WikiPageDump extends util.Iterator[WikiPage] with Closeable {
  protected var LOG = LoggerFactory.getLogger(this.getClass)
  protected var dumpFile: String = null

  /**
    * Get dump file name for this dump object, such as seq.gz, 20150702
    * .page-article.gz
    *
    * @return
    */
  final def getDumpName: String = dumpFile

  def open(): Try[Unit]

  def reset(): Unit = {
    close()
    open()
  }

//  /**
//    * Traverse page dump file with filters
//    *
//    * @param filters
//    */
//  final def traverse(filters: WikiPageFilter*): Try[Unit] = {
//    LOG.info("Use " + this.getClass.getSimpleName + " to traverse " + dumpFile)
//    open()
//    var index: Int = 0
//    val counter: ProgressCounter = new ProgressCounter
//    while ( {
//      hasNext
//    }) {
//      val page: WikiPage = next
//      for (filter <- filters) {
//        filter.process(page, index)
//      }
//      index += 1
//      counter.increment()
//    }
//    for (filter <- filters) {
//      filter.close()
//    }
//    close()
//    LOG.info("Done for traverse " + dumpFile + ", " + index + " pages has " + "been visited.")
//  }
}