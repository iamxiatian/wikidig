package wiki.dig.algorithm

import better.files.File
import breeze.stats.distributions.Rand
import wiki.dig.db.{CategoryDb, CategoryHierarchyDb}

import scala.collection.mutable
import scala.util.Random

/**
  * 生成训练用数据集，每个数据集C由一个子图、子图所拥有的文章构成。
  *
  * 每次生成一个子图后，子图的信息通过父子节点对(父节点, 子节点）的形式保存到文本文件中。
  * 然后生成文档集合，根据子图节点所拥有的文章数量进行有重复的放回抽样得到。每篇文章会
  * 记录其在子图中的路径，同一文章可能会拥有不同的路径。
  */
object CorpusGenerator {
  def generate(sampleCount: Int, corpusFile: String = "./corpus.txt"): Unit = {
    val writer = File(corpusFile).newPrintWriter()
    (1 to sampleCount) foreach { idx =>
      val text = generateOne(idx)
      writer.println(text)
      writer.flush()
      if (idx % 100 == 0) {
        println(s"process $idx / $sampleCount ... ")
      }
    }
    writer.close()
    println("DONE!")
  }

  def generateOne(corpusId: Int): String = {
    //生成一个均值为50，标准差为10的高斯分布
    val g = breeze.stats.distributions.Gaussian(50, 10)

    val graph = SubGraphGenerator.generate(g.sample().toInt).toSeq
    SubGraphGenerator.toDotFile(graph, "/tmp/test.dot")
    Runtime.getRuntime.exec(s"dot -Tpng /tmp/test.dot -o /tmp/test.png")

    //生成一个均值为1000，标准差为100的高斯分布
    val g2 = breeze.stats.distributions.Gaussian(1000, 100)

    val nodeText = graph.flatMap { p => Seq(p._1, p._2) }.distinct.mkString(",")
    val edgeText = graph.map {
      case (f, t) => s"$f-$t"
    }.mkString(",")

    val articleText = sampleArticles(graph, g2.sample().toInt).map {
      case (id, path) =>
        s"$id\t$path"
    }.mkString("\n")

    s"""
       |corpus $corpusId
       |nodes: $nodeText
       |edges: $edgeText
       |articles:
       |$articleText
     """.stripMargin
  }

  /**
    * 抽样子图中的文章, 返回文章与对应路径的列表
    */
  def sampleArticles(graph: Seq[(Int, Int)], sampleSize: Int): Seq[(Int, String)] = {
    //存储父节点的索引信息
    val parentIndex: Map[Int, Seq[Int]] = graph.groupBy(_._2) //按照孩子分组
      .map {
      case (child, parents) =>
        //转换为(孩子节点, 父节点集合)
        (child, parents.map(_._1))
    }

    val nodeIds = graph.flatMap { p => Seq(p._1, p._2) }.distinct

    //分类节点上的文章数量
    val counts: Seq[Int] = nodeIds.map {
      id =>
        if (CategoryHierarchyDb.startNodeIds.contains(id))
          0
        else {
          //println(s"==> ${CategoryDb.getNameById(id).get}: ${CategoryDb.getPageCount(id).getOrElse(0)}")
          val depth = CategoryHierarchyDb.getCNode(id).get.depth
          //深度越大越容易选中，父节点（路径）越多，越容易被选中
          CategoryDb.getPageCount(id).getOrElse(0) * depth * parentIndex.get(id).map(_.size).getOrElse(1)
        }
    }


    //子图抽取的文章数量分布，主键为子图上的节点ID，值为该节点上需要抽取的文章数量
    val pickedDist = mutable.Map.empty[Int, Int]

    //设置抽样的文章所在的节点，保存在pickedDist中
    (1 to sampleSize) foreach { _ =>
      //该子图所拥有的文章总数量
      val randNumber = Random.nextInt(counts.sum)
      var accumulator = 0

      //选中的子图节点
      val pickedId: Int = nodeIds.zip(counts).find {
        case (id, c) =>
          if (c + accumulator > randNumber)
            true
          else {
            accumulator += c
            false
          }
      }.map(_._1).getOrElse(nodeIds.last)

      //pickedId的计数器加1
      pickedDist(pickedId) = pickedDist.getOrElse(pickedId, 0) + 1
    }

    val pickedArticles: Seq[(Int, String)] = pickedDist.toSeq.flatMap {
      case (cid, count) =>
        //从节点id中抽文章
        val pickedIds = (1 to count) map {
          _ =>
            val pageIds = CategoryDb.getPages(cid)
            Rand.choose(pageIds).get()
        }

        pickedIds.map {
          id =>
            //val path = getPath(cid, parentIndex).map(CategoryDb.getNameById(_).get)
            val path = getPath(cid, parentIndex)
            (id, path.mkString(","))
        }
    }

    pickedArticles
  }

  /**
    * 获取子类的完整路径
    */
  private def getPath(cid: Int, parentIndex: Map[Int, Seq[Int]]): Seq[Int] =
    parentIndex.get(cid) match {
      case Some(parents) =>
        val parentId = Rand.choose(parents).get()
        getPath(parentId, parentIndex) :+ cid
      case None => Seq(cid)
    }

  def main(args: Array[String]): Unit = {
    println("RUN: bin/corpus-generator <size> <filename>")
    println("E.g.: bin/corpus-generator 10000 corpus.txt")

    if (args.length == 2) {
      val sampleCount = args(0).toInt
      generate(sampleCount, args(1))
    } else {
      println("Wrong parameters.")
    }
  }
}
