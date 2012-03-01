package com.pathdependent.github

import scala.collection.mutable
import org.eclipse.egit.github.core.Repository

case class FetchedRepo (
  var id: Long,
  var repository: Repository = null,
  var watchers: mutable.Set[FetchedUser] = mutable.Set(),
  var collaborators: mutable.Set[FetchedUser] = mutable.Set(),
  var collected: Boolean = false
) {
  /**
   * @note I use lots of sets. The hashCode of a case class is a function of
   *       the class parameters. This is inappropriate, given that each 
   *       repository is uniquely identified by its id alone.
   */
  override def hashCode() = id.hashCode
}

object FetchedRepo {
  val serialVersionUID = 87657654567888L
}
