package wiki.dig.algorithm

import java.io.File
import java.nio.charset.StandardCharsets

import com.google.common.io.Files
import wiki.dig.db.{CategoryDb, CategoryHierarchyDb}

import scala.util.Random

/**
  * 生成论文需要的数据集合: 每一个小的语料库(corpus)的抽取策略：
  *
  * 随机挑选一个分类节点，Random Walk到相邻节点，继续Random Walk, 直到选够节点数量为止。
  *
  * First, pick one
  *
  */
object Generator {

  trait Direction

  case object Up extends Direction

  case object Down extends Direction

  import StandardCharsets.UTF_8

  import CategoryHierarchyDb.startNodeIds

  /**
    * 挑选一个随机种子，后面会根据该种子节点，不断随机跳转得到一个子图。
    *
    * @return
    */
  def seed(): Int = {
    Random.nextInt(startNodeIds.length)
  }

  /**
    * 类别cid的孩子和父亲构成的相邻节点
    *
    * @param cid
    * @return
    */
  def neighborIds(cid: Int): Seq[(Int, Direction)] = {
    val children = CategoryHierarchyDb.getCNode(cid).toSeq.flatMap(_.childLinks)
    val parents = CategoryHierarchyDb.getParentIds(cid)

    children.map((_, Down)) ++ parents.map((_, Up))
  }

  def randomWalk(N: Int): Unit = {
    val depthNumbers: Seq[Int] = (1 to 6).toSeq

    //在第几层停止跳转
    val directCountDist: Seq[Long] = depthNumbers.map {
      d =>
        CategoryHierarchyDb.articleCountAtDepth(d).get.directCount.toLong
    }

    val writer = Files.newWriter(new File("./path.txt"), UTF_8)

    depthNumbers.zip(directCountDist).foreach {
      case (d, c) =>
        println(s"articles on depth $d ==> $c")
    }

    var count = 0
    while (count < N) {
      //本次生成的路径长度
      val pathLength = pick(depthNumbers, directCountDist)

      val cids = generatePath(if (pathLength > 2) pathLength else 3)

      val articleIds = CategoryDb.getPages(cids.last)
      if (articleIds.nonEmpty) {
        count += 1
        val rand = Random.nextInt(articleIds.length)
        val pickedArticleId = articleIds(rand)
        writer.write(s"$pickedArticleId\t${cids.mkString("\t")}\n")
        if (count % 1000 == 0) {
          println(s"$count / $N ")
          writer.flush()
        }
      } else {
        //println(s"empty articles: path len: $pathLength, category: ${cids.last}")
      }
    }
    writer.close()
  }

  def generatePath(pathLength: Int): Seq[Int] = {

    /**
      * 从当前的节点里面，选择一个跳转，返回选中的节点，以及下一步的候选集合
      */
    def walk(nodeIds: Seq[Int]): (Int, Seq[Int]) = {
      if (nodeIds.isEmpty)
        (0, Seq.empty[Int])
      else {
        val weights: Seq[Long] = nodeIds.map {
          id =>
            CategoryHierarchyDb.getArticleCount(id).map(_.recursiveCount).getOrElse(0L)
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
  def pick(elements: Seq[Int], weights: Seq[Long]): Int = {
    val total = weights.sum
    if (total == 0) {
      elements(Random.nextInt(elements.size))
    } else {
      val randNumber = Random.nextDouble()
      val scores = weights.map(_ / (total.toDouble))
      var accumulator = 0.0

      elements.zip(scores).find {
        case (e, s) =>
          if (s + accumulator > randNumber)
            true
          else {
            accumulator += s
            false
          }
      }.map(_._1).getOrElse(elements.head)
    }
  }

  def main(args: Array[String]): Unit = {
    val cid = 690747

    println(CategoryDb.getNameById(690747))

    neighborIds(cid).foreach {
      case (id, d) =>
        val name = CategoryDb.getNameById(id)
        println(s"$id \t $name ($d)")
    }
  }
}
