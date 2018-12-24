package wiki.dig.algorithm

import wiki.dig.db.CategoryHierarchyDb

/**
  * 生成文件的路径，生成方法：从第一级节点开始，以RandomWalk的方式，随机挑选路径
  * 在每一级别上，以随机概率判断是否停止跳跃。
  *
  */
class PathGenerator {

  def randomWalk(): Unit = {
    //在第几层停止跳转
    val depthDist = (1 to 5).map {
      d => (d, CategoryHierarchyDb.articleCountAtDepth(d).get)
    }.toMap

    val total = depthDist.map(_._2.directCount).sum

    val stopDepth = 5
  }

  def main(args: Array[String]): Unit = {

  }
}
