package com.pathdependent.github

import scala.collection.JavaConversions._
import scala.collection.mutable

import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.client._
import org.eclipse.egit.github.core.service._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

/** 
 * The collector is EXTREMELY sloppy, but I have no time now, and we need to 
 * start collecting data NOW, because the time alotted to analysis is already
 * being used up. Since I won't be touching the serialized case classes any
 * more, I can clean as I get time.
 *
 * Seriously, this shit is ugly.
 */
object CollectData {
  var apiHits = 0
  var nextReset: DateTime = null
  var db: Database = null
  val client = new GitHubClient()
  
  def main(args: Array[String]) {
    parseArgs(args)
    
    // Keep after the database loading. Although it is small now, the load time
    // will be significant later on.
    nextReset = DateTime.now + 1.hours
  }
  
  def parseArgs(args: Array[String]) {
    args match {
      case Array(userName, password, databasePath) => 
        client.setCredentials(userName, password)
        println("Loading database: this may take a while...")
        db = Database.load(databasePath)
        
      case _ =>
        println("Usage: CollectData username password database")
        System.exit(0)
    }
    
    if(db.users.size == 0) { 
      ProminentUsers foreach db.getUser 
      db.save()
    }
    
    fetchAll()
  }
  
  def fetchAll() {
    var userFetchQueue = mutable.Stack[FetchedUser]() ++ db.uncollectedUsers
    var repoFetchQueue = mutable.Stack[FetchedRepo]() ++ db.uncollectedRepos
    
    var fetchCounter = 1
    do {
      if(fetchCounter % 3 > 0) {
        if(userFetchQueue.isEmpty) { 
          userFetchQueue pushAll db.uncollectedUsers 
        }
        
        if(!userFetchQueue.isEmpty) {
          safeAPIRequest(collectUser(userFetchQueue.pop.name))
        }
        
      } else {
        if(repoFetchQueue.isEmpty) { 
          repoFetchQueue pushAll db.uncollectedRepos 
        }
        
        if(!repoFetchQueue.isEmpty) {
          safeAPIRequest(collectRepo(repoFetchQueue.pop.id))
        }
      }
      fetchCounter += 1
    } while ( !userFetchQueue.isEmpty || !repoFetchQueue.isEmpty )
    
    db.save()
  }
  
  def safeAPIRequest(request: => Unit) {
    // The technical limit is 5000 per hour, but I didn't see a straight-forward
    // way to figure out how to view the RateLimit http header in the library
    // I'm using, so this *should* work while being polite.
    if(apiHits >= 4950) { 
      db.save() // Why not, we're not doing anything else.
      resetAPICounter() 
    }
    
    try {
      request
    } catch {
      case e =>
        System.err.println(e)
        e.printStackTrace()
        resetAPICounter()
    }
  }
  
  /**
   * More sloppiness!
   */ 
  def resetAPICounter() {
    System.out.println("Users collected: " + db.users.size)
    System.out.println("Repos collected: " + db.repos.size)
    
    if(nextReset > DateTime.now) {
      System.out.println("Sleeping until: " + nextReset)
      Thread.sleep((new Duration(DateTime.now, nextReset)).getMillis)
    }
    
    nextReset = DateTime.now + 1.hours
    apiHits = 0
  }
  
  def collectUser(userName: String) {
    println("Fetching User: " + userName + " " + apiHitsAnonnotation)
    val fetchedUser = db.getUser(userName)
    
    // collection the user object, if nessessary.
    if(!fetchedUser.user.isDefined) {
      apiHits += 1
      val userService = new UserService(client)
      fetchedUser.user = Some(userService.getUser(userName))
    }
    
    // collect the users repositories, if nessessary.
    if(fetchedUser.repositories.size == 0) {
      val repositoryService = new RepositoryService(client)
      val repos = repositoryService.getRepositories(userName).toList
      apiHits += (repos.length / 100.0).ceil.toInt max 1
      repos.foreach { repo =>
        val fetchedRepo = db.getRepo(repo.getId)
        if(fetchedRepo.repository == null) { 
          fetchedRepo.repository = repo
        }
        fetchedUser.repositories += fetchedRepo
      }
    }
    
    fetchedUser.collected = true
  }
  
  def apiHitsAnonnotation() = "(" + (5000 - apiHits) + ")"
  
  def collectRepo(id: Long) {
    println("Fetching Repo: " + id + " " + apiHitsAnonnotation)
    
    // The repository object was (should be?) already filled in by collectUser.
    val fetchedRepo = db.getRepo(id)
    
    // Collect the collaborators, if nessesary.
    if(fetchedRepo.collaborators.size == 0) {
      val collaboratorService = new CollaboratorService(client)
      val collaborators = collaboratorService.
        getCollaborators(fetchedRepo.repository)
      
      apiHits += (collaborators.length / 100.0).ceil.toInt max 1
      
      collaborators.foreach { user => 
        val fetchedUser = db.getUser(user.getLogin)

        if(!fetchedUser.user.isDefined) {
          fetchedUser.user = Some(user)
        }

        fetchedUser.collaborations += fetchedRepo

        fetchedRepo.collaborators += fetchedUser
      }
    }
  
    // Collect the watchers, if nessessary. You are always watching your repo,
    // so == 0 is a good test rather than an Option.
    if(fetchedRepo.watchers.size == 0) {
      val watcherService = new WatcherService(client)
      
      val watchers = watcherService.getWatchers(fetchedRepo.repository)

      apiHits += (watchers.length / 100.0).ceil.toInt max 1
      
      watchers.foreach { user => 
        val fetchedUser = db.getUser(user.getLogin)

        if(!fetchedUser.user.isDefined) {
          fetchedUser.user = Some(user)
        }

        fetchedUser.watching += fetchedRepo
        
        fetchedRepo.watchers += fetchedUser
      }
    }
  }
}
