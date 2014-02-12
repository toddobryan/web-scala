package util

import models.auth.User
import org.joda.time.DateTime

sealed trait TestableItem {
  // NOTE: ID is only accessible AFTER an item is added to the database.
  def id: Long
  def title: String
  def owner: User
  def parentId: Long
}

trait TestableDirectory extends TestableItem

trait TestableFile extends TestableItem {
  def content: String
  def tests: String
  def lastModified: Option[DateTime]
}