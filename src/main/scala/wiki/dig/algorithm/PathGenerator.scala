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
      d => (d, CategoryHierarchyDb.articleCountAtDepth(d))
    }.toMap

    val total = depthDist.map(_._2).sum



    val d1 = CategoryHierarchyDb.articleCountAtDepth(1)
    val d2 = CategoryHierarchyDb.articleCountAtDepth(2)
    val d3 = CategoryHierarchyDb.articleCountAtDepth(3)
    val d4 = CategoryHierarchyDb.articleCountAtDepth(4)
    val d5 = CategoryHierarchyDb.articleCountAtDepth(5)


    val stopDepth = 5
  }

  def main(args: Array[String]): Unit = {

  }
}
