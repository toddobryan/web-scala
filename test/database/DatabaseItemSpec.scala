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
  
  
  
  def withFixture(test: OneArgTest): Outcome = {
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
      
      // The first two items are added, should be found. The last one should not be found.
      itemDb.getItemById(root1.id) shouldBe (Some(root1))
      itemDb.getItemById(file1.id) shouldBe (Some(file1))
      itemDb.getItemById(root2.id) shouldBe (None)
    }
  }
}  