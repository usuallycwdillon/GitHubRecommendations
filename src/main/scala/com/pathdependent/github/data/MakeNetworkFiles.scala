package com.pathdependent.github

import java.io._
import scala.collection.mutable
import scala.io.Source

/**
 * I think it probably makes sense to find the computationally "lightest"
 * metric, rather than the most complicated one. See Netflix blog post about
 * not using best algorithm.
 *
 * The main method is split into three different generators because their is 
 * some wierd GC issue about running them all together. Maybe the closure 
 * does something quirky with the large map. I'm not sure. 
 */
object MakeNetworkFiles {
  def main(args: Array[String]) {

    args match {
      case Array("--contributor") =>

        generateRelationshipLinkedRepositories(
          "contributor_linked_repositories.tsv", 
          loadRelationships("user_contributes_to_repository.tsv")
        )

      case Array("--watching") =>

        generateRelationshipLinkedRepositories(
          "watch_linked_repositories.tsv", 
          loadRelationships("user_watches_repository.tsv")
        ) 

      case Array("--owner") =>
        generateRelationshipLinkedRepositories(
          "owner_linked_repositories.tsv", 
          loadRelationships("user_owns_repository.tsv")
        )

      case _ => 
        println("Usage: MakeNetworkFiles --owner|watching|contributor")
    }

  }

  def generateRelationshipLinkedRepositories(
    outputFile: String, relationships: Map[String, Set[Int]]
  ) {
    var repositoryNetwork = mutable.Map.empty[(Int,Int), Int]

    relationships foreach { case (user, repositories) =>
      // The a > b is a cheap hack to 1) not double count and 2) exclude links
      // to self.
      for(a <- repositories; b <- repositories; if a > b) {
        val edge = a -> b
        repositoryNetwork += edge -> (repositoryNetwork.getOrElse(edge, 0) + 1)
      }
    } 

    OutputData.writeWith(outputFile) { f =>
      f.println("Repository.A\tRepository.B\tN")
      repositoryNetwork foreach { case ((a,b), n) => 
        f.println("%d\t%d\t%d".format(a,b,n))
      }
    }
  }

  def loadRelationships(fileName: String): Map[String, Set[Int]] = {
    val edges = mutable.Map.empty[String, Set[Int]]

    loadTSV(fileName) foreach { row =>
      val user = row(0)
      val repository = row(1).toInt

      val contributions = edges.getOrElseUpdate(user, Set.empty[Int])
      edges(user) = contributions + repository
    }

    edges.toMap
  }

  def loadTSV(fileName: String) = {
    val source = Source.fromFile(fileName)
    val lines = source.getLines.toList
    val rows = lines.tail.map(_.split("\t"))
    source.close()
    rows
  }
}

