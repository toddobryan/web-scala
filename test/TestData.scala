import models.auth._
import models.files._
import scalajdo._

object TestData {
  val user1: User = new Teacher("jmiller14", "Jim", "Miller", email="jmiller@nada.com", password="temp123")
  val user2: User = new Student("amaniraj14", "Aaditya", "Manirajan", password="temp123")
  val user3: User = new User("cjin14", "Choong Won", "Jin")
  val users = List(user1, user2, user3)

  val assignment: Assignment = new Assignment("Basic Scala", "val x = 0",
      """
      val test1 = x == 4
      val myTests = List(("Val of x", test1, ""))
      """)
  
   val r4: Block = (user1, user2) match {
    case (t: Teacher, s: Student) => new Block("R4", t, List(assignment), List(s))
  }
  
  def load() {
    val dbFile = new java.io.File("data.h2.db")
    if (dbFile.exists) dbFile.delete()
    DataStore.execute { tpm =>
        for(u <- users) {
          tpm.makePersistent(u)
        }
        //tpm.makePersistent(r4)
    }
  }
}