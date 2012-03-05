package com.pathdependent.github

import scala.collection.mutable
import org.eclipse.egit.github.core.{Gist,User}

case class FetchedUser (
  var name: String,
  var user: Option[User] = None,
  var repositories: mutable.Set[Long] = mutable.Set(),
  var collaborations: mutable.Set[Long] = mutable.Set(),
  var watching: mutable.Set[Long] = mutable.Set(),
  var followers: mutable.Set[String] = mutable.Set(),
  var gists: mutable.Set[Long] = mutable.Set(),
  var organizations: mutable.Set[String] = mutable.Set(),
  var collected: Boolean = false
) {
/**
   * @note I use lots of sets. The hashCode of a case class is a function of
   *       the class parameters. This is inappropriate, given that each 
   *       user is uniquely identified by its name alone.
   */
  override def hashCode() = name.hashCode
  
  def this() { this(null) }
}

object FetchedUser {
  val serialVersionUID = 123456789876543L
  
  val Schema = """CREATE TABLE users (
    user_name VARCHAR PRIMARY KEY,
    serialized_object VARBINARY,
    fetched BOOLEAN
  )"""
}
