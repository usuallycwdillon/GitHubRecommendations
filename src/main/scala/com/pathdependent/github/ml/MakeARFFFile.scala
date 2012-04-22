package com.pathdependent.github.ml

import com.pathdependent.github.MakeNetworkFiles.loadTSV
import java.io._
import org.joda.time.{Days, DateTime}
import org.joda.time.format.DateTimeFormat
import scala.collection.mutable
import scala.io.Source

object MakeARRFFile {
  val userNames = mutable.Map.empty[String,String]
  val users = mutable.Map.empty[String, User]
  val repositories = mutable.Map.empty[Int, Repository]
  val contributorGraph = new RepositoryGraph
  val watcherGraph = new RepositoryGraph

  def loadData() {
    // E.G.: Thu Aug 25 03:12:35 EDT 2011
    val pattern = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss YYYY")
    val lastCollectionDate = new DateTime(2012, 3, 25, 10, 52)
    def daysAgo(s: String) = Days.daysBetween(
      pattern.parseDateTime(s.replace("EDT ","").replace("EST ", "")),
      lastCollectionDate
    ).getDays
    
    println("Loading repo meta data...")
    // Load the repository meta data first 
    val source = Source.fromFile("repo_meta.tsv")
    val reader = source.getLines
    reader.next() // Skip header
    for(line <- source.getLines; column = line.split("\t"); if line != "") {
      repositories(column(0).toInt) = Repository(
        language = column(10),
        isFork = column(1) == "true",
        hasWiki = column(4) == "true",
        ageInDays = daysAgo(column(5)),
        openIssues = column(7).toInt,
        hasDownloads = column(2) == "true",
        numberOfWatchers = column(9).toInt,
        numberOfForks = column(6).toInt
      )
    }
    source.close()

    println(repositories.keys.max)

    // Now load the watcher and contributor edges.
    println("Loading contributor links...")
    loadTSV("user_contributes_to_repository.tsv") foreach { 
      case Array(userNameRaw, repositoryIdStr) =>
        // Slight reduction in memory usage, hopefully.
        val userName = userNames.getOrElseUpdate(userNameRaw, userNameRaw)
        val repositoryId = repositoryIdStr.toInt
        val repository = repositories(repositoryId)

        users.
          getOrElseUpdate(userName, new User).
          contributesTo(repositoryId, repository)

        //repository.contributors += userName
        repository.numberOfContributors += 1
    }

    repositories.values foreach { _.reduceMemoryUsage }

    println("Loading watcher links...")
    loadTSV("user_watches_repository.tsv") foreach { 
      case Array(userNameRaw, repositoryIdStr) =>
        val userName = userNames.getOrElseUpdate(userNameRaw, userNameRaw)
        val repositoryId = repositoryIdStr.toInt
        val repository = repositories(repositoryId)

        users.
          getOrElseUpdate(userName, new User).
          watchesRepositoryIds += repositoryId

        //repository.watchers += userName
    }

    println("Loading ownership links...")
    loadTSV("user_owns_repository.tsv") foreach { 
      case Array(userNameRaw, repositoryIdStr) =>
        val userName = userNames.getOrElseUpdate(userNameRaw, userNameRaw)
        val repositoryId = repositoryIdStr.toInt

        users.
          getOrElseUpdate(userName, new User).
          ownsRepositoryIds += repositoryId
    }

    println("Building contributor graph...")

    users foreach { case (name, user) =>
      for(
        a <- user.contributesToRepositoryIds; 
        b <- user.contributesToRepositoryIds;
        if a != b
      ) {
        contributorGraph.addToEdge(a, b, name)
      }
    }
    println(contributorGraph.edges.size)

    println("Building watcher graph...")

   /* users foreach { case (name, user) =>
      for(
        a <- user.watchesRepositoryIds; 
        b <- user.watchesRepositoryIds;
        if a != b
      ) {
        watcherGraph.addToEdge(a, b, name)
      }
    }*/
    println(watcherGraph.edges.size)
  }

  val Header = """
@relation contributors

@attribute is_user_familiar_with_language { true, false }
@attribute user_watches_repository { true, false }
@attribute repo_is_fork { true, false }
@attribute repo_has_wiki { true, false }
@attribute repo_age numeric
@attribute repo_open_issues numeric
@attribute repo_has_downloads { true, false }
@attribute repo_watchers numeric
@attribute repo_contributors numeric
@attribute repo_forks numeric
@attribute n_unweighted_direct_contributor_links numeric
@attribute n_weighted_direct_contributor_links numeric
@attribute n_unweighted_once_removed_contributor_links numeric
@attribute n_weighted_once_removed_watcher_links numeric

@attribute will_contribute? { true, false }

@data
"""

  def main(args: Array[String]) {
    loadData()

    val allRepoIds = repositories.keys.toArray

    var i = 0
    writeWith("/media/My Passport/GitHub/contributes.arff") { f => 
      f.println(Header)

      // Only use users who have ever contributed. It's a motivation and talent
      // issue.
      for((userName,user) <- users.par) {
        if(user.hasContributions) {
          println(i + " " + users.size)
          i += 1
          // ignore contributor relations when the user is the owner of the 
          // repository.
          var n = 0
          for(
            repoId <- user.contributesToRepositoryIds; 
            if !user.ownsRepositoryIds.contains(repoId)
          ) {
            val row = makeRecord(userName, user, repoId, true)
            f synchronized { f.println(row) }

            n += 1
          }

          val nonContributing = mutable.Set.empty[Int]
          while(nonContributing.size < n) {
            val repoId = allRepoIds(scala.util.Random.nextInt(allRepoIds.size))
            if(!user.contributesToRepositoryIds.contains(repoId)) {
              nonContributing += repoId
            }
          }

          nonContributing foreach { repoId => 
            val row = makeRecord(userName, user, repoId, false)

            f synchronized { f.println(row) }
          }
        }
      }
    }
  }

  def makeRecord(userName: String, user: User, repositoryId: Int, isContributor: Boolean) = {
    val repository = repositories(repositoryId)
    val distance = contributorGraph.distanceCalculation(
      userName, user, repositoryId
    )
    // Build the distance between the user and the repository.

    "%b,%b,%b,%b,%d,%d,%b,%d,%d,%d,%f,%f,%f,%f,%b".format(
      user.familiarLanguages contains repository.language,
      user.watchesRepositoryIds contains repositoryId,
      repository.isFork,
      repository.hasWiki,
      repository.ageInDays,
      repository.openIssues,
      repository.hasDownloads,
      repository.numberOfWatchers,
      repository.numberOfContributors,
      repository.numberOfForks,
      distance._1, distance._2, 0.0, 0.0,
      isContributor
    )
  }

  def writeWith(fileName: String)(f: PrintStream => Unit): Unit = {
    println("Writing to " + fileName)
    val output = new PrintStream(new FileOutputStream(fileName))

    try {
      f(output)
    } finally {
      output.close()
    }
  }
}
