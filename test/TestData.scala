import models.auth._
import models.files._
import scalajdo._

object TestData {
  val user1: User = new User("jmiller14")
  val user2: User = new User("amaniraj14")
  val user3: User = new User("cjin14")
  val users = List(user1, user2, user3)
  
  val file1: File = new File("First File", user1,
		  					 "val x = 2")
  val file2: File = new File("Another File", user2,
		  					 "class Dog { def bark = \"woof!\"")
  val file3: File = new File("Third File", user3,
		  					 "abstract class Animal")
  val files = List(file1, file2, file3)
  
  
  def load = {
    val dbFile = new java.io.File("data.h2.db")
    dbFile.delete()
    DataStore.execute { tpm =>
        for(u <- users) {
          tpm.makePersistent(u)
        }
        for(f <- files) {
          tpm.makePersistent(f)
        }
    }(null)
  }
}