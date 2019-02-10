package wiki.dig.algorithm

import wiki.dig.db.CategoryDb

object CorpusGenerator {
  def generate(): Unit = {
    val graph = SubGraphGenerator.generate(30).toSeq
    val nodeIds = graph.flatMap { p => Seq(p._1, p._2) }.distinct
    //分类节点上的文章数量
    val countDist: Map[Int, Int] = nodeIds
      .map(id => (id, CategoryDb.getPageCount(id).getOrElse(0)))
      .toMap


  }
}
