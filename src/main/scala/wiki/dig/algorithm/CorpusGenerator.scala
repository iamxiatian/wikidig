package wiki.dig.algorithm

import wiki.dig.db.CategoryDb

import scala.util.Random

/**
  * 生成训练用数据集，每个数据集C由一个子图、子图所拥有的文章构成。
  *
  * 每次生成一个子图后，子图的信息通过父子节点对(父节点, 子节点）的形式保存到文本文件中。
  * 然后生成文档集合，根据子图节点所拥有的文章数量进行有重复的放回抽样得到。每篇文章会
  * 记录其在子图中的路径，同一文章可能会拥有不同的路径。
  */
object CorpusGenerator {
  def generate(): Unit = {
    val graph = SubGraphGenerator.generate(30).toSeq
    val nodeIds = graph.flatMap { p => Seq(p._1, p._2) }.distinct

    //分类节点上的文章数量
    val counts: Seq[Int] = nodeIds.map { id =>
      CategoryDb.getPageCount(id).getOrElse(0)
    }

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


  }
}
