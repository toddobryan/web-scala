package models.files

import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import org.joda.time.DateTime
import scalajdo._
import models.auth._
import scala.tools.nsc.interpreter.{ Results => IntpResults }
import webscala._
import util.{ UsesDataStore, TestableItem, TestableFile, TestableDirectory }

@PersistenceCapable(detachable="true")
sealed abstract class Item {
  @PrimaryKey
  @Persistent(valueStrategy=IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  private[this] var _title: String = _
  @Persistent(defaultFetchGroup = "true")
  private[this] var _owner: User = _
  private[this] var _parentId: Long = _
  @Persistent(defaultFetchGroup= "true")
  private[this] var _lastModified: java.sql.Timestamp = _
  
  def id: Long = _id
  
  def title: String = _title
  def title_=(theTitle: String) = (_title = theTitle)
  
  def owner: User = _owner
  def owner_=(theOwner: User) = (_owner = theOwner)
  
  def parentId: Long = _parentId
  def parentId_=(theId: Long) = (_parentId = theId)
  
  def lastModified: Option[DateTime] = if(_lastModified == null) None else Some(new DateTime(_lastModified.getTime))
  def lastModified_=(theDate: DateTime) = (_lastModified = new java.sql.Timestamp(theDate.getMillis()))
  
   def timeString = lastModified match {
    case None => ""
    case Some(date) => {
      val now = DateTime.now()
      val timeSince = now.getMillis - date.getMillis
      val millInADay = 86400000
      if(timeSince < millInADay && now.getDayOfMonth == date.getDayOfMonth)
        "%d:%02d".format(date.getHourOfDay, date.getMinuteOfHour)
      else if (timeSince < 2 * millInADay && now.getDayOfMonth - 1 == date.getDayOfMonth)
        "Yesterday" + "%d:%02d".format(date.getHourOfDay, date.getMinuteOfHour)
      else
        "%02d-%02d".format(date.getDayOfMonth, date.getMonthOfYear)
    }
  }
}

trait QItem extends PersistableExpression[Item] {
  private[this] lazy val _id: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_id")
  def id: NumericExpression[Long] = _id

  private[this] lazy val _title: StringExpression = new StringExpressionImpl(this, "_title")
  def title: StringExpression = _title

  private[this] lazy val _owner: ObjectExpression[User] = new ObjectExpressionImpl[User](this, "_owner")
  def owner: ObjectExpression[User] = _owner
  
  private[this] lazy val _parentId: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_parentId")
  def parentId: NumericExpression[Long] = _parentId
}

object QItem {
  def apply(parent: PersistableExpression[Item], name: String, depth: Int): QItem = {
    new PersistableExpressionImpl[Item](parent, name) with QItem
  }

  def apply(cls: Class[Item], name: String, exprType: ExpressionType): QItem = {
    new PersistableExpressionImpl[Item](cls, name, exprType) with QItem
  }

  private[this] lazy val jdoCandidate: QItem = candidate("this")

  def candidate(name: String): QItem = QItem(null, name, 5)

  def candidate(): QItem = jdoCandidate

  def parameter(name: String): QItem = QItem(classOf[Item], name, ExpressionType.PARAMETER)

  def variable(name: String): QItem = QItem(classOf[Item], name, ExpressionType.VARIABLE)
}

class Directory extends Item with TestableDirectory with DisplayableDirectory {
  def this(title: String, owner: User, parentId: Long) {
    this()
    title_=(title)
    owner_=(owner)
    parentId_=(parentId)
  }
}

object Directory extends UsesDataStore {
  def create(title: String, owner: User, parentId: Long): Directory = {
    val dir = new Directory(title, owner, parentId)
    dataStore.pm.makePersistent(dir)
  }
  
  def getById(id: Long): Option[Directory] = {
    val cand = QItem.candidate
    val query = dataStore.pm.query[Item].filter(cand.id.eq(id)).executeOption
    query match {
      case Some(dir: Directory) => Some(dir)
      case _ => None
    }
  }
  
  // TODO: Need to fix this
  def getItems(directory: Directory): List[Item] = Nil
  
  // TODO: And this
  def getItem(pathToFile: List[String]): Option[Item] = None

  def getByOwner(owner: User): List[Directory] = {
   Nil
  }
  
  def getUserRoot(user: User): Directory = { new Directory() /*
    val cand = QDirectory.candidate
    dataStore.pm.query[Directory].filter(cand.owner.eq(user).and(cand.parentId.eq(0.toLong))).executeOption() match {
      case Some(root) => root
      case None => {
        val root = new Directory("Home", user, 0)
        dataStore.pm.makePersistent(root)
        root
      }
    }*/
  }
}

class File extends Item with TestableFile with DisplayableFile with UsesDataStore {  
  
  def this(title: String, owner: User, parentId: Long) {
    this()
    title_=(title)
    owner_=(owner)
    parentId_=(parentId)
  }
  
  protected def fileaddon: FileAddOn = {
    val cand = QFileAddOn.candidate
    try {
      dataStore.pm.query[FileAddOn].filter(cand.itemId.eq(id)).executeOption.get
    } catch {
      case e: Exception => throw new Exception("This file was not created with an add-on.")
    }
  }
  
  def content: String = fileaddon.content
  def content_=(theContent: String): Unit = {
    fileaddon.content_=(theContent)
    dataStore.pm.makePersistent(fileaddon)
  }
  
  def tests: String = fileaddon.tests
  def tests_=(theTests: String): Unit = {
    fileaddon.tests_=(theTests)
    dataStore.pm.makePersistent(fileaddon)
  }
  
  
  override def toString = {
    "%s -- %s".format(title, owner)
  }
  
  def testName: String = File.objectName(title) + "Test"
  
  def recentSort(file: File): Boolean = {
    lastModified match {
      case None => false
      case Some(dt1) => file.lastModified match {
        case None => true
        case Some(dt2) => dt1.getMillis > dt2.getMillis
      }
    }
  }
  
  def isAssignment(currPath: String): Boolean = {
    val pathList = currPath.split("/").toList
    if(pathList.length > 2) false
    else {
      val maybeBlock = owner match {
          case t: Teacher => Block.getByTeacher(t).find(_.name == pathList.head)
          case s: Student => {
            Block.getByStudent(s).find(_.name == pathList.head)
          }
      }
      Assignment.getBlockAssignments(
          maybeBlock.getOrElse(new Block("", new Teacher(""), Nil))
      ).exists(_.title == title)
    }
  }
  
  def runOwnTest: String = {
    val setupResult = SafeCode.runCode {
        val setupCode = 
          """
          import java.io._
          import scala.Console
          
          val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
          """
        (new HtmlRepl).repl.interpret(setupCode)
    }
    setupResult._1 match {
      case IntpResults.Success => {
        val printoutCode = 
          """
          Console.withOut(baos) {
          """ +
          "(new " + testName + ").execute()" +
          """
          }
          
          val output: String = new String(baos.toByteArray, "UTF-8")
          """
        (new HtmlRepl).repl.interpret(printoutCode) match {
          case IntpResults.Success => {
                (new HtmlRepl).repl.valueOfTerm("output") match {
                  case Some(x) => testResultsToHtmlString(x.toString)
                  case _ => "There was an error in retrieving your test results."
                }
          }
          case _ => "A problem occurred while running your tests. Check that the name of your test object is " + testName + "."
        }
      }
      case _ => "WebScala messed up! Contact an administrator with the following error:\n" + setupResult._2
    }
  }
  
  def testResultsToHtmlString(results: String): String = {
    val resultList = results.split("\\[0m").toList
    def resultProcessor(res: String): String = {
      if(res.contains("[32m")) "<div class=\"success-result\">" + res.substring(4) + "</div>"
      if(res.contains("[33m")) "<div class=\"warning-result\">" + res.substring(4) + "</div>"
      if(res.contains("[31m")) "<div class=\"failure-result\">" + res.substring(4) + "</div>"
      else "<span>" + res.length + "</span>" + "<span>" + res + "</span>"
    }
    {for(result <- resultList) yield resultProcessor(result)}.mkString("<br \\>")
  }
}
  
object File extends UsesDataStore {
  def create(title: String, owner: User, parentId: Long, 
      content: String, test: String): File = {
    val f = new File(title, owner, parentId)
    dataStore.pm.makePersistent(f)
    val addon = new FileAddOn(f.id, content, test)
    dataStore.pm.makePersistent(addon)
    f
  }
  
  def defaultFile: String = "/* Insert Code Here */"
	
  def defaultTestCode(s: String): java.lang.String =
	"""import org.scalatest.FunSuite
	  |import org.scalatest.matchers.ShouldMatchers
	  |
	  |// Do not change the name of this test object, please!
	  |class """ + objectName(s) + "Test" + """ extends FunSuite with ShouldMatchers {
	  |  test("sample") {
	  |    val x = 2 + 2
	  |   x should be === (4)
	  |  }
	  |}""".stripMargin
	
	
  def objectName(s: String): String = {
	s.split(" ").map(capitalize(_)).mkString("")
  }

  def capitalize(s: String) = {
    s.replaceFirst(s.substring(0,1), s.substring(0,1).toUpperCase)
  }
}

@PersistenceCapable(detachable="true")
class FileAddOn {
  @PrimaryKey
  @Persistent(valueStrategy=IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  @Unique
  @Column(allowsNull="false")
  private[this] var _itemId: Long = _
  @Column(length=1048576) // 1MB
  private[this] var _content: String = _
  @Column(length=1048576) // 1MB
  private[this] var _tests: String = _
  
  def this(itemId: Long, content: String, tests: String) = {
    this()
    itemId_=(itemId)
    content_=(content)
    tests_=(tests)
  }
  
  def id: Long = _id
  
  def itemId: Long = _itemId
  def itemId_=(theItemId: Long) = (_itemId = theItemId)
  
  def content: String = _content
  def content_=(theContent: String) = (_content = theContent)
  
  def tests: String = _tests
  def tests_=(theTests: String) = (_tests = theTests)
}

trait QFileAddOn extends PersistableExpression[FileAddOn] {
  private[this] lazy val _id: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_id")
  def id: NumericExpression[Long] = _id
  
  private[this] lazy val _itemId: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_itemId")
  def itemId: NumericExpression[Long] = _itemId
  
  private[this] lazy val _content: StringExpression = new StringExpressionImpl(this, "_content")
  def content: StringExpression = _content
  
  private[this] lazy val _tests: StringExpression = new StringExpressionImpl(this, "_tests")
  def tests: StringExpression = _tests
}

object QFileAddOn {
  def apply(parent: PersistableExpression[FileAddOn], name: String, depth: Int): QFileAddOn = {
    new PersistableExpressionImpl[FileAddOn](parent, name) with QFileAddOn
  }
  
  def apply(cls: Class[FileAddOn], name: String, exprType: ExpressionType): QFileAddOn = {
    new PersistableExpressionImpl[FileAddOn](cls, name, exprType) with QFileAddOn
  }
  
  private[this] lazy val jdoCandidate: QFileAddOn = candidate("this")
  
  def candidate(name: String): QFileAddOn = QFileAddOn(null, name, 5)
  
  def candidate(): QFileAddOn = jdoCandidate
  
  def parameter(name: String): QFileAddOn = QFileAddOn(classOf[FileAddOn], name, ExpressionType.PARAMETER)
  
  def variable(name: String): QFileAddOn = QFileAddOn(classOf[FileAddOn], name, ExpressionType.VARIABLE)
}
