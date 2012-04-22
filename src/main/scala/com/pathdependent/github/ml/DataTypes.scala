package com.pathdependent.github.ml

import com.pathdependent.github.MakeNetworkFiles.loadTSV
import java.io._
import org.joda.time.{Days, DateTime}
import org.joda.time.format.DateTimeFormat
import scala.collection.mutable
import scala.io.Source

/**
 * My workstation is FUBAR so I have to code this so that memory is less of 
 * an issue. It's ugly; it's breakable; it's typical acadamic work. 
 *
 * @note I'm using Ints instead of Longs because it should result in
 *       significant savings and the maximum value found so far fits in an int
 *       ...assuming i find a cheap way to do unboxed ints...
 */

class User {
  val contributesToRepositoryIds = mutable.Set.empty[Int]
  val watchesRepositoryIds = mutable.Set.empty[Int]
  val ownsRepositoryIds = mutable.Set.empty[Int]
  val familiarLanguages = mutable.Set.empty[String]

  def hasContributions = contributesToRepositoryIds.size > 0

  def contributesTo(id: Int, repository: Repository) {
    contributesToRepositoryIds += id
    familiarLanguages += repository.language
  }
}

case class Repository (
  language: String, 
  isFork: Boolean,
  hasWiki: Boolean,
  ageInDays: Int,
  openIssues: Int,
  hasDownloads: Boolean,
  numberOfWatchers: Int,
  numberOfForks: Int
) {
  //val watchers = mutable.Set.empty[String]
  //val contributors = mutable.Set.empty[String]
  var numberOfContributors = 0

  def reduceMemoryUsage() {
  //  numberOfContributors = contributors.size
  //  contributors.clear()
  }
}

class RepositoryGraph {
  val edges = mutable.Map.empty[(Int, Int), mutable.Set[String]]
  //val edges = mutable.Map.empty[Long, mutable.Set[String]]

  def addToEdge(a: Int, b: Int, userName: String) {
    // This is pretty annoying stuff to spend my time on. Basically,
    // the a->b is packed into a 64 bit long. This means instead of 
    // 3 required objects -- the tuple, the int, and the int -- it only
    // needs 1 object -- the long. Additionally, scala may (i'm not sure)
    // provide a specialized map for long values. It will save A LOT of bytes,
    // especially relative to the size of the primative types.
    //var x = a.toLong << 32 | b
    //edges.getOrElseUpdate(x, mutable.Set.empty[String]) += userName
    edges.getOrElseUpdate(a->b, mutable.Set.empty[String]) += userName
  }

  def distanceCalculation(userName: String, user: User, repositoryId: Int) = {
    val potentialLinks = user.
      contributesToRepositoryIds.filterNot(_ == repositoryId)

    var unweightedOne, weightedOne = 0.0

    if(potentialLinks.size > 0) {
      edges.foreach { case ((a,b), members) => 
        if(potentialLinks.contains(a) && b == repositoryId) {
          val nMembers = if(members contains userName) {
            members.size - 1
          } else { 
            members.size
          }

          if(nMembers > 0) {
            unweightedOne += 1.0
            weightedOne += nMembers
          }
        }
      }
      // Does a direct link exist between a user and a repository?
      // Does a once removed link exist between a user and a repository?

      unweightedOne /= potentialLinks.size
      weightedOne /= potentialLinks.size
    }

    (unweightedOne, weightedOne)
  }
}