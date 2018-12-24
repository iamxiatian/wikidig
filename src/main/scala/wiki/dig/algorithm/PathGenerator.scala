package wiki.dig.algorithm

import wiki.dig.db.{CategoryDb, CategoryHierarchyDb}

import scala.util.Random

/**
  * 生成文件的路径，生成方法：从第一级节点开始，以RandomWalk的方式，随机挑选路径
  * 在每一级别上，以随机概率判断是否停止跳跃。
  *
  */
class PathGenerator {

  def randomWalk(): Unit = {
    val depthNumbers: Seq[Int] = (1 to 6).toSeq

    //在第几层停止跳转
    val depthDist = depthNumbers.map {
      d => (d, CategoryHierarchyDb.articleCountAtDepth(d).get)
    }.toMap

    val directCountDist: Seq[Int] = depthDist.map(_._2.directCount).toSeq

    (1 to 100) foreach {
      _ =>
        //本次生成的路径长度
        val pathLength = pick(depthNumbers, directCountDist)
        val cids = generatePath(pathLength)

        val articleIds = CategoryDb.getPages(cids.last)
        val rand = Random.nextInt(articleIds.length)
        val pickedArticleId = articleIds(rand)

        println(s"$pickedArticleId\t$cids")
    }
  }

  def generatePath(pathLength: Int): Seq[Int] = {

    /**
      * 从当前的节点里面，选择一个跳转，返回选中的节点，以及下一步的候选集合
      */
    def walk(nodeIds: Seq[Int]): (Int, Seq[Int]) = {
      if (nodeIds.isEmpty)
        (0, Seq.empty[Int])
      else {
        val weights = nodeIds.map {
          id =>
            CategoryHierarchyDb.getArticleCount(id).map(_.recursiveCount.toInt).getOrElse(0)
        }

        val id = pick(nodeIds, weights)
        val childNodes = CategoryHierarchyDb.getCNode(id).get.childLinks
        (id, childNodes)
      }
    }

    var nodeIds = CategoryHierarchyDb.startNodeIds

    (1 to pathLength).map {
      _ =>
        val (c, children) = walk(nodeIds)
        nodeIds = children
        c
    }.toSeq
  }

  /**
    * 根据随机数，挑选一个落在权重范围里面的元素。例如：elements为[1,2,3,4,5]，
    * 权重为[100,3,2,10,9]， 生成的随机数为3，则落在第一个元素上，返回第一个元素1.
    */
  def pick(elements: Seq[Int], weights: Seq[Int]): Int = {
    val total = weights.sum
    val randNumber = Random.nextInt(total)
    var accumulator = 0

    elements.zip(weights).find {
      case (e, w) =>
        if (w + accumulator > randNumber)
          true
        else {
          accumulator += w
          false
        }
    }.map(_._1).getOrElse(elements.head)
  }

  def main(args: Array[String]): Unit = {

  }
}
