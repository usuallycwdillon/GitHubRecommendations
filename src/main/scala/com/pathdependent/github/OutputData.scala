package com.pathdependent.github

import java.io._

object OutputData {
  var db: Database = null
  val OutputDirectory = new File(".")

  def main(args: Array[String]) {
    parseArgs(args)

    outputUserOwnsRepository()
    outputUserContributesToRepository()
    outputUserWatchesRepository()
    
    outputUserMeta()
    outputRepositoryMeta()
  }

  def parseArgs(args: Array[String]) {
    args match {
      case Array(databasePath) => 
        println("Loading database: this may take a while...")
        db = Database.load(databasePath)
        
      case _ =>
        println("Usage: OutputData database")
        System.exit(0)
    }
  }

  def outputUserOwnsRepository() {
    writeWith("user_owns_repository.tsv") { output =>
      output.println("User\tRepository.ID")
      db.users.values foreach { user =>
        user.repositories.foreach { repositoryID =>
          output.println("%s\t%d".format(user.name, repositoryID))
        }
      }
    }
  }

  def outputUserContributesToRepository() {
    writeWith("user_contributes_to_repository.tsv") { output =>
      output.println("User\tRepository.ID")
      db.repos.values foreach { repository =>
        repository.collaborators.foreach { userName =>
          output.println("%s\t%d".format(userName, repository.id))
        }
      }
    }
  }

  def outputUserWatchesRepository() {
    writeWith("user_watches_repository.tsv") { output =>
      output.println("User\tRepository.ID")
      db.repos.values foreach { repository =>
        repository.watchers.foreach { userName =>
          output.println("%s\t%d".format(userName, repository.id))
        }
      }
    }
  }

  def outputUserMeta() {
    writeWith("user_meta.tsv") { output =>
      output.println(
        "Name\tLogin\tIs.Hirable\tCreated.At\tN.Collaborators\t" +
        "Disk.Usage\tN.Followers\tN.Public.Repos\tBlog.Url\t" + 
        "Company\tEmail\tLocation"
      )
      db.users.values foreach { fetchedUser =>
        fetchedUser.user foreach { user => 
          output.println(
            "%s\t%s\t%b\t%s\t%d\t%d\t%d\t%d\t%s\t%s\t%s\t%s".format(
              user.getName,
              user.getLogin,
              user.isHireable,
              user.getCreatedAt,
              user.getCollaborators,
              user.getDiskUsage,
              user.getFollowers,
              user.getPublicRepos,
              user.getBlog,
              user.getCompany,
              user.getEmail,
              user.getLocation
            )
          )
        }
      }
    }
  }

  def outputRepositoryMeta() {
    writeWith("repo_meta.tsv") { output =>
      output.println(
        "ID\tIs.Fork\tHas.Downloads\tHas.Issues\tHas.Wiki\tCreated.At\tN.Forks\tN.Open.Issues\tSize\tN.Watchers\tHomepage\tLanguage"
      )
      db.repos.values foreach { fetchedRepository =>
        val repo = fetchedRepository.repository
        if(repo != null) {
          output.println(
            "%d\t%b\t%b\t%b\t%b\t%s\t%d\t%d\t%s\t%s\t%s".format(
              repo.getId,
              repo.isFork,
              repo.isHasDownloads,
              repo.isHasIssues,
              repo.isHasWiki,
              repo.getCreatedAt,
              repo.getOpenIssues,
              repo.getForks,
              repo.getSize,
              repo.getWatchers,
              repo.getHomepage,
              repo.getLanguage,
              repo.getName
            )
          )
        }
      }
    }
  }

  def writeWith(fileName: String)(f: PrintStream => Unit): Unit = {
    println("Writing to " + fileName)
    val output = new PrintStream(
      new FileOutputStream(new File(OutputDirectory, fileName))
    )

    try {
      f(output)
    } finally {
      output.close()
    }
  }
}