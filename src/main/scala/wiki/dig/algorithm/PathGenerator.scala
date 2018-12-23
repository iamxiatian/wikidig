package wiki.dig.algorithm

import wiki.dig.db.CategoryHierarchyDb

import scala.io.StdIn

/**
  * 生成文件的路径，生成方法：从第一级节点开始，以RandomWalk的方式，随机挑选路径
  * 在每一级别上，以随机概率判断是否停止跳跃。
  *
  */
class PathGenerator {

  def walk(): Unit = {

  }

  def main(args: Array[String]): Unit = {
    println("Prepare to calculate article count...")
    StdIn.readLine()

    CategoryHierarchyDb.calculateArticleCount()
  }
}
