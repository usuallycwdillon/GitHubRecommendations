package com.pathdependent.github

import java.io._
import scala.collection.mutable

/**
 * @note I have a lot of RAM. This database is <i>in memory</i>. As it gets
 *       bigger, my project collaborators may have trouble. This probably
 *       won't run on my MacBook Air.
 * 
 * @note Uses the underlying Java Serialization API for saving the file. 
 *       The Eclipse library was kind enough to make all the objects
 *       serializable. Rather than extracting what I think I want to use now,
 *       I'll save everything in case I change my mind later. Obviously,
 *       this file will get big, but I don't think the serialization API has
 *       size limitations -- it will just get slow. If it does have a problem
 *       I'll convert the saved file to something usable if needed.
 */
case class Database(fileName: String) {
  val users = mutable.Map.empty[String, FetchedUser]
  val repos = mutable.Map.empty[Long, FetchedRepo]

  //transient val userIdToName = mutable.Map.empty[Int, String]

  def save() {
    val output = new ObjectOutputStream(new FileOutputStream(fileName))
    output.writeObject(this);
    output.close();
  }
  
  def uncollectedUsers() = users.values.filterNot(_.collected)
  
  def uncollectedRepos() = repos.values.filterNot(_.collected)
  
  def getUser(userName: String): FetchedUser = {
    users getOrElseUpdate (userName, FetchedUser(userName)) 
  }
  
  def getRepo(id: Long): FetchedRepo = {
    repos getOrElseUpdate (id, FetchedRepo(id))
  }
}

object Database {
  val serialVersionUID = 9876765456L

  def load(fileName: String): Database = {
    try {
      val input = new ObjectInputStream(new FileInputStream(fileName))
      val database = input.readObject.asInstanceOf[Database]
      input.close()
      database
    } catch {
      case _:java.io.EOFException => Database(fileName)
    }
  }
}
