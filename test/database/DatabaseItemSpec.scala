package test.database

import models.auth._
import models.files._
import scalajdo._
import util.UsesDataStore
import org.scalatest.fixture
import org.scalatest.Outcome
import org.scalatest.Matchers._
import scala.language.postfixOps
import scala.util.{Try, Success, Failure}
import util.{TestableDirectory, TestableFile, TestableItem}

class DatabaseItemSpec extends fixture.FunSpec with UsesDataStore {
  
  type FixtureParam = ItemDatabaseInterface
  
  
<<<<<<< HEAD
  type OneArgTest = fixture.Suite.
  
  def withFixture(test: OneArgTest) {
=======
  
  def withFixture(test: OneArgTest): Outcome = {
>>>>>>> c627179dd7782e3c1f813e3f098bfc3343fc2872
    object CurrentImplementation extends ItemDatabaseInterface {
      /* I fill this in once I complete the class updates. */
      def newItem(item: TestableItem) = ()
      def newItems(items: List[TestableItem]) = ()
      def deleteFile(itemId: Long) = ()
      def deleteDirectory(itemId: Long) = ()
      def getItemById(id: Long) = None
      def getItemsByTitleForUser(title: String, user: User) = Nil
      def getItems(directory: TestableDirectory) = Nil
      def getItem(directory: TestableDirectory, pathToFile: List[String]) = None
      def makeUserRoot(user: User) = Failure(new Exception("This is unimplemented."))
      def getUserRoot(user: User) = None
      def makeFile(title: String, owner: User, 
          parentId: Long, content: String, tests: String) = new File()
      def makeDirectory(title: String, owner: User, parentId: Long) = new Directory()
    }
    test(CurrentImplementation)  
  }
  
  describe("An Item Database") {
    it("should be able to add and find items") { itemDb =>
      import itemDb.SampleTestItems._
      
      // Add two sample items for testing
      itemDb.newItem(root1)
      itemDb.newItem(file1)
      itemDb.newItem(root3)
      itemDb.newItem(file3)
      itemDb.newItem(file4)
      
      
      // The first two items are added, should be found. The last one should not be found.
      itemDb.getItemById(root1.id) shouldBe (Some(root1))
      itemDb.getItemById(file1.id) shouldBe (Some(file1))
      itemDb.getItemById(root2.id) shouldBe (None)
<<<<<<< HEAD
      itemDb.getItemById(file2.id) shouldBe (None)
      itemDb.getItemById(file3.id) shouldBe (Some(file3))
      itemDb.getItemById(root3.id) shouldBe (Some(root3))
      
       //tests method newItems 
      itemDb.newItems(twofolders)
      itemDb.getItemById(folder1.id) shouldBe (folder1)
      itemDb.getItemById(folder2.id) shouldBe (folder2)
      
      itemDb.newItems(folderwithfiles)
      itemDb.getItemById(folder3.id) should be (folder3)
      itemDb.getItemById(file5.id) should be (file5)
      itemDb.getItemById(file6.id) should be (file6)
      
      
      //tests getItemsByTitleForUser
      itemDb.getItemsByTitleForUser(file1.title, file1.owner) shouldBe (Some(file1))
      itemDb.getItemsByTitleForUser(file5.title, file5.owner) shouldBe (Some(file5))
      itemDb.getItemsByTitleForUser(file6.title, file6.owner) shouldBe (Some(file6))
      itemDb.getItemsByTitleForUser(folder2.title, folder2.owner) shouldBe (Some(folder2))
      itemDb.getItemsByTitleForUser(folder3.title, folder3.owner) shouldBe (Some(folder3))
      
      
      //tests deleteFile
      itemDb.deleteFile(file3.id)
      itemDb.getItemById(file3.id) shouldBe (None)
      
      //tests deleteDirectory
      itemDb.deleteDirectory(root3.id)
      itemDb.getItemById(root3.id) shouldBe (None)
      itemDb.getItemById(file4.id) shouldBe (None)
      
      //tests getItemById
      itemDb.getItemById(folder1.id) shouldBe (Some(folder1))
      itemDb.getItemById(folder2.id) shouldBe (Some(folder2))
      itemDb.getItemById(folder3.id) shouldBe (Some(folder3))
      itemDb.getItemById(file5.id) shouldBe (Some(file5))
      itemDb.getItemById(file6.id) shouldBe (Some(file6))
     
      //tests getItems
      itemDb.getItems(folder3) shouldBe (List(file6, file7))
      itemDb.getItems(folder2) shouldBe (List(file5))
      itemDb.getItems(folder1) shouldBe (Nil)
      itemDb.getItems(root3) shouldBe (List(file3, file4))
      
      //tests getItem
      
      itemDb.getItem(folder1, List(s"$folder2.title, $folder3.title, $file6.title")) shouldBe (file6)
      
      
      
      
      
=======
>>>>>>> c627179dd7782e3c1f813e3f098bfc3343fc2872
      
      
    }
  }
}  