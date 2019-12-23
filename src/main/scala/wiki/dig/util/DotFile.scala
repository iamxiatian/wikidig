package wiki.dig.util

import better.files.File
import wiki.dig.algorithm.keyword.ArticleDataset

import scala.util.{Failure, Success, Try}

/**
  * GraphViz的dot格式的文件生成工具
  */
object DotFile extends Logging {
  /**
    * 将pair数据集，转换为dot语法，输出到dot文件中，方便可视化呈现
    * DOT语法参考Graphviz
    */
  def toDotFile(triples: Seq[(String, String, Int)], dotFile: String): Unit = {
    //出现的所有的名称
    val names = triples.flatMap(t => List(t._1, t._2)).toSet.toSeq

    // 名称对应的下标作为id
    val ids: Seq[Int] = (1 to names.size)

    //名称和下标的映射
    val nameIdMapping: Map[String, Int] = names.zip(ids).toMap

    val tips: Seq[String] = ids.zip(names).map {
      case (id, name) =>
        s"""$id [label="$name", fontname="FangSong"];"""
    }

    val nodeText = tips.mkString("\n")
    val edgeText = triples.map {
      case (first, second, cnt) => s"""${nameIdMapping.get(first).get} -- ${nameIdMapping.get(second).get}[label="${cnt}"];"""
    }.mkString("\n")

    //    val dotText =
    //      s"""
    //         |digraph g {
    //         |  graph [ordering="out"];
    //         |  margin=0;
    //         |  $nodeText
    //         |  $edgeText
    //         |}
    //    """.stripMargin

    val dotText =
      s"""
         |graph g {
         |  graph [ordering="out"];
         |  margin=0;
         |  $nodeText
         |  $edgeText
         |}
    """.stripMargin

    val f = File(dotFile)
    f.parent.createDirectoryIfNotExists(true)

    f.writeText(dotText)
    Try {
      Runtime.getRuntime.exec(s"dot -Tpng ${dotFile} -o ${dotFile.replace(".dot", ".png")}")
    } match {
      case Success(_) =>

      case Failure(exception) =>
        LOG.error("save dot file error", exception)
    }
    // Runtime.getRuntime.exec("dot -Tpdf /tmp/tree.dot -o /tmp/tree.pdf")
    //Runtime.getRuntime.exec(s"dot -Tpng /tmp/tree-$i.dot -o /tmp/test-$i.png")
  }

  def main(args: Array[String]): Unit = {
    //toDotFile(Seq(("中国", "人民"), ("中国", "上海"), ("中国", "北京")), "test.dot")
    ArticleDataset.toDotFile(1, "test.dot")

  }
}
