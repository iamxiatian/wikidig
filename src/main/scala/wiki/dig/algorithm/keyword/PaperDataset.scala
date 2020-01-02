package wiki.dig.algorithm.keyword

import java.io.FileReader

import breeze.io.CSVReader
import ruc.irm.extractor.keyword.graph.PositionWordGraph
import wiki.dig.util.DotFile

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
  * 中文图情领域期刊的标题、摘要和关键词数据集
  */
object PaperDataset {
  val papers: IndexedSeq[Paper] = {
    val reader = new FileReader("./data/paper_abstract.csv")
    val csvReader = CSVReader.read(reader, skipLines = 1)
    val results = csvReader.map {
      r =>
        Paper(r(0), r(1).split(";"), r(2))
    }
    reader.close()
    results
  }

  def count() = papers.length

  def get(idx: Int) = papers(idx)

  def main(args: Array[String]): Unit = {
    papers.foreach(p => println(p.title))
  }


  def toDotFile(id: Int, dotFile: String): Unit = {
    val paper = get(id)
    val g = new PositionWordGraph(1, 0, 0, true)
    g.build(paper.title, 30)
    g.build(paper.`abstract`, 1.0f)
    g.findTopKeywords(5)

    def score(name: String) = {
      val s = g.getWordNode(name).getScore.formatted("%.3f")
      s"$name($s)"
    }

    val edges: Seq[DotFile.Edge] = g.getWordNodeMap.asScala.toSeq
      .sortBy(_._2.getCount)(Ordering.Int.reverse)
      //.take(100)
      .flatMap {
        case (name, node) =>
          //转换为二元组对：（词语，词语右侧相邻的词语）
          val pairs1: Seq[DotFile.Edge] = node.getLeftNeighbors.asScala.map {
            case (adjName, cnt) =>
              DotFile.Edge(score(adjName), score(name), cnt.toInt, 1)
          }.toSeq

          //          val pairs2: Seq[(String, String, Int)] = node.getRightNeighbors.asScala.map {
          //            case (adjName, cnt) =>
          //              (name, adjName, cnt.toInt)
          //          }.toSeq
          //
          //          pairs1 ++: pairs2
          pairs1
      }

    //记录已经存在的边，避免把近邻边重复加入到图中
    val existedEdges = mutable.Set.empty[String]
    edges.foreach { edge =>
      existedEdges += s"${edge.src}-${edge.dest}"
      existedEdges += s"${edge.dest}-${edge.src}"
    }

    val edges2: Seq[DotFile.Edge] = g.getWordNodeMap.asScala.toSeq
      .sortBy(_._2.getCount)(Ordering.Int.reverse)
      //.take(100)
      .flatMap {
        case (name, node) =>
          //转换为二元组对：（词语，词语右侧相邻的词语）
          node.getAdjacentWords.asScala.map {
            case (adjName, cnt) =>
              DotFile.Edge(score(adjName), score(name), cnt.toInt, 2)
          }.toSeq
      }
      .filter { edge =>
        //过滤掉已经存在的边
        if (existedEdges.contains(s"${edge.src}-${edge.dest}")) {
          false
        } else {
          existedEdges += s"${edge.src}-${edge.dest}"
          existedEdges += s"${edge.dest}-${edge.src}"
          true
        }
      }

    DotFile.save(edges ++: edges2, dotFile)
  }
}

case class Paper(title: String, tags: Seq[String], `abstract`: String)
