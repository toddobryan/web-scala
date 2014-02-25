package test.database

import models.auth.User
import org.joda.time.DateTime
import scala.util.Try
import util._

trait ItemDatabaseInterface {
  /** 
   *  This method should add a created item to the database.
   *  After calling this method, item should be able to be 
   *  queried for and searched for using methods like
   *  getItemById.
   */
  def newItem(item: TestableItem): Unit
  /**
   * This method should make every item in the list items
   * available in the database, meeting the conditions 
   * specified in the above method.
   */
  def newItems(items: List[TestableItem]): Unit
  /**
   * After calling this method, the file with the item ID should no longer be
   * accessible by the database. If the fileId does not correspond
   * to a file in the database or corresponds to a directory,
   * then an Exception should be thrown.
   */
  def deleteFile(itemId: Long): Unit
  /**
   * After calling this method, the directory ID should no longer be
   * accessible by the database, and all items that are contained inside
   * the directory should also be removed from the database.
   */
  def deleteDirectory(itemId: Long): Unit
  
  /**
   * This method should return Some(item), where item corresponds to the id.
   * If those items are not available, should return None.
   */
  def getItemById(id: Long): Option[TestableItem]
  /**
   * This method should return a list of all items
   * that match the title string for the user. Nil if
   * none exist 
   */
  def getItemsByTitleForUser(title: String, user: User): List[TestableItem]
  /**
   * This method should query the database for when item.parentId == directory.id
   * and also item.owner == directory.owner.
   * Basically every item that has a parentId that matches the
   * directory's ID, when they both have the same owner.
   * If no items match, Nil.
   */
  def getItems(directory: TestableDirectory): List[TestableItem]
  /**
   * This method should start in the directory listed and then try to find the directory
   * with the title that is at the head of the pathToFile List.
   * Ex: getItem(dir1, List("Directory 2", "Directory 3", "The File"))
   *  would check dir1 for a directory with title "Directory 2", and then would
   *  check that directory for a directory with title "Directory 3" and then 
   *  would return Some(f) where f is the file with title "The File"
   *  If any of the items don't exist, return None.
   */
  def getItem(directory: TestableDirectory, pathToFile: List[String]): Option[TestableItem]
  /**
   * Creates a new directory for the user with the title "Home", and 
   * a parentId of 0 and corresponding owner. If the user already has a root,
   * this should return Failure(exception). Otherwise, it should return
   * Success(dir) where dir is the new Directory.
   * NOTE: This does NOT add the user root to the database. That still has to
   * be done with add item.
   */
  def makeUserRoot(user: User): Try[TestableDirectory]
  /**
   * This method should return Some(dir) where dir.owner == user
   * and dir.parentId == 0. If it does not exist, this should return None.
   */
  def getUserRoot(user: User): Option[TestableDirectory]
  
  ///////// We don't need to test these method. While I'm cleaning up //////////////
  ///////// the classes, we don't have set constructors so these help //////////////
  ///////// with the tests.											  ////////////// 
  
  /**
   * Just like a constructor. Takes in these parameters and makes a file to test with.
   */
  def makeFile(title: String, owner: User, parentId: Long, content: String, tests: String): TestableFile
  /**
   * Just like a constructor. Takes in these parameters and makes a directory to test with.
   */
  def makeDirectory(title: String, owner: User, parentId: Long): TestableDirectory
  
  
  /**
   * If you want some test items, build them here
   */
  object SampleTestItems {
    val user1: User = new User("jmiller14", "Jim", "Miller")
    val user2: User = new User("erosentrom15", "Erik", "Rosenstrom")
    val user3: User = new User("jhouse14", "John", "House")
    val user4: User = new User("rnarain14", "Raghav", "Narain")
    val user5: User = new User("arosenstrom15", "Andrew", "Rosenstrom")
    val user6: User = new User("jafable14", "Juan", "Afable")
    val user7: User = new User("rshah14", "Ryan", "Shah")
    val user8: User = new User("modersky100", "Mark", "O'Dersky")
    val user9: User = new User("tobryan", "Todd", "O'Bryan")
    val user10: User = new User("ekoston100", "Eric", "Koston")
    val user11: User = new User("mmo100", "Mike", "Mo")
    val user12: User = new User("smalto100", "Sean", "Malto")
    val user13: User = new User("mtaylor100", "Mikey", "Taylor")
    val user14: User = new User("prod84", "Paul", "Rodrigeuz")
    val user15: User = new User("tpudwill100", "Torey", "Pudwill")
    val user16: User = new User("mkubiak15", "Mark", "Kubiak")
        
    
    val root1: TestableDirectory = makeDirectory("Home", user1, 0)
    val root2: TestableDirectory = makeDirectory("Home", user2, 0)
    val root3: TestableDirectory = makeDirectory("Home", user3, 0)
    
    val file1: TestableFile = makeFile("Recursion Ex.", user1, root1.id, "val x = \"Jim\"", "No tests")
    val file2: TestableFile = makeFile("Images and Animations", user2, root2.id, "val img = new Image()", "test { }")
  }
}