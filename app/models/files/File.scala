package models.files

import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import org.joda.time.DateTime
import scalajdo._
import models.auth._
import scala.tools.nsc.interpreter.{ Results => IntpResults }
import webscala.HtmlRepl

abstract class Item {
  def title: String
  def owner: User
  def asHtmlFO: scala.xml.Elem
  def asHtmlFO(path: String): scala.xml.Elem
  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem
  
  def pathLinks(path: String): List[String] = {
    val splitPath = path.split("/").toList
    if(splitPath.head != "") {
      (for(i <- 1 to splitPath.length) yield (splitPath.take(i).mkString("/"))).toList
    }
    else Nil
  }

  def topOfFile(path: String, user: User): scala.xml.Elem = 
    <ul class="breadcrumb">
	  <li><a href="/fileManager">Home</a> <span class="divider">/</span></li>
      {for(l <- pathLinks(path)) yield Item.breadcrumbLink(l, user)}
    </ul>
}

object Item {
  def breadcrumbLink(link: String, user: User): scala.xml.Elem =
    <li>
    <a href={"/fileManager/" + link}>
    {link.split("/").toList.last} 
    </a>
    <span class="divider">/</span>
    </li>
}

@PersistenceCapable(detachable="true")
class File extends Item {
  @PrimaryKey
  @Persistent(valueStrategy=IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  private[this] var _title: String = _
  @Persistent(defaultFetchGroup = "true")
  private[this] var _owner: User = _
  @Column(length=1048576) // 1MB
  private[this] var _content: String = _
  @Persistent(defaultFetchGroup= "true")
  private[this] var _lastModified: java.sql.Timestamp = _
  
  def this(title: String, owner: User, content: String, lastModified: Option[DateTime] = None) {
    this()
    _title = title
    _owner = owner
    _content = content
    lastModified match {
      case None => _lastModified = null
      case Some(date) => lastModified_=(date)
    }
  }
  
  def id: Long = _id
  
  def title: String = _title
  def title_=(theTitle: String) = (_title = theTitle)
  
  def owner: User = _owner
  def owner_=(theOwner: User) = (_owner = theOwner)
  
  def content: String = _content
  def content_=(theContent: String) = (_content = theContent)
  
  def lastModified: Option[DateTime] = if(_lastModified == null) None else Some(new DateTime(_lastModified.getTime))
  def lastModified_=(theDate: DateTime) = (_lastModified = new java.sql.Timestamp(theDate.getMillis()))
  
  override def toString = {
    "%s -- %s".format(title, owner)
  }
  
  def recentSort(file: File): Boolean = {
    lastModified match {
      case None => false
      case Some(dt1) => file.lastModified match {
        case None => true
        case Some(dt2) => dt1.getMillis > dt2.getMillis
      }
    }
  }
  
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
  
  // F.0. -> File Organizer
  def asHtmlFO: scala.xml.Elem = {
    <li class="file-fo">
	  <a href={"/file/" + id}>
		<div class="file-title span6">{title}</div>
		<div class="file-modified span2">{timeString}</div>
      </a>
      <form class="in-line">
		<button method="post" action={"/deleteFile/" + id}>
			Delete?
		</button>
	  </form>
    </li>
  }
  
  def asHtmlFO(pathToDir: String): scala.xml.Elem = {
    <li class="file-fo">
	  <a href={"/fileManager/" + pathToDir + {if(pathToDir == "") "" else "/"} + title}>
		<div class="file-title span6">{title}</div>
		<div class="file-modified span2">{timeString}</div>
      </a>
    </li>
  }
  
  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem = {
     <li class={"file-fo"}>
       <a href={"/myClasses/" + blockString + "/" + studentString + "/" + {if(pathToDir == "") "" else "/"} + title}>
         <div class="file-title span4">{title}</div>
		 <div class="file-modified span2">{timeString}</div>
       </a>
	   <div class="span3"><button href={teacherTestUrl(blockString, studentString, pathToDir)} class="btn test-btn">Test Code</button></div>
     </li>
   }
  
  def teacherTestUrl(blockString: String, studentString: String, pathToDir: String): String ={
    "/myClasses/" + blockString + "/" + studentString + "/" + "testCode" + "/" + {if(pathToDir == "") "" else "/"} + title
  }
  
  def isAssignment(currPath: String): Boolean = {
    val pathList = currPath.split("/").toList
    if(pathList.length > 2) false
    else {
      val maybeBlock = 
        owner match {
          case t: Teacher => Block.getByTeacher(t).find(_.name == pathList.head)
          case s: Student => {
            println(Block.getByStudent(s))
            println(Block.getAll)
            Block.getByStudent(s).find(_.name == pathList.head)
          }
      }
      maybeBlock.getOrElse(new Block("", new Teacher(""), Nil, Nil)).assignments.exists(_.title == title)
    }
  }
  
  def runTests(assignment: Assignment): scala.xml.Elem  = {
    HtmlRepl.repl.reset()
    println(content)
    val errorStart1 = HtmlRepl.out.getBuffer.length
    val contentResult = HtmlRepl.repl.interpret(content)
    val contentError = HtmlRepl.out.getBuffer.substring(errorStart1)
    testMatcher(contentResult, "compiling your file", contentError) {
      val errorStart2 = HtmlRepl.out.getBuffer.length
      val assignmentResult = HtmlRepl.repl.interpret(assignment.testCode)
      val assignmentError = HtmlRepl.out.getBuffer.substring(errorStart2)
      testMatcher(assignmentResult, "compiling the assignment tests", assignmentError + " This could be an error on your teacher's part, or you may have failed to define a required function.") {
        val errorStart3 = HtmlRepl.out.getBuffer.length
        val testerResult = HtmlRepl.repl.interpret(HtmlRepl.webscalaTester)
        val testerError = HtmlRepl.out.getBuffer.substring(errorStart3)
        testMatcher(testerResult, "preparing the test results", testerError + "The class's teacher may not have set up the tests correctly.") {
          val theTests = HtmlRepl.repl.valueOfTerm(HtmlRepl.repl.mostRecentVar)
          if(HtmlRepl.isCorrectType(theTests)) renderTestResults(HtmlRepl.unwrapTests(theTests))
          else renderTestResults(List(("Report this error to miller.james01@gmail.com", false)))
        }
      }
    }
  }
  
  def testMatcher(result: IntpResults.Result, occurrence: String, additionalDiagnostic: String = "")
  				 (successAction: scala.xml.Elem): scala.xml.Elem = {
    result match {
      case IntpResults.Success => successAction
      case IntpResults.Incomplete => renderTestResults(List(("No errors, but the code used while " + occurrence + " was incomplete. Check your syntax.", false)))
      case _ => renderTestResults(List(("The following error occurred while " + occurrence + ": " +
    		  						    additionalDiagnostic, false)))	
    }
  } 
  
  def renderTestResults(results: List[(String, Boolean)]): scala.xml.Elem = {
    results match {
      case Nil => <div class="alert alert-info">No tests were run</div>
      case results => {
        <div id="result">
    	  {for(result <- results) yield <div class={"alert " + {if(result._2) "alert-success" else "alert-error"}}>{result._1}</div>}
    	</div>
      }
    }
  }
}
  
object File {
	def getById(id: Long): Option[File] = {
	  val cand = QFile.candidate
	  DataStore.pm.query[File].filter(cand.id.eq(id)).executeOption
	}
	
	def getByOwner(owner: User): List[File] = {
	  val cand = QFile.candidate
	  DataStore.pm.query[File].filter(cand.owner.eq(owner)).executeList
	}
	
	def mostRecentFour(owner: User): List[File] = {
	  getByOwner(owner).sortWith(_ recentSort _).take(4)
	}
}

trait QFile extends PersistableExpression[File] {
  private[this] lazy val _id: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_id")
  def id: NumericExpression[Long] = _id
  
  private[this] lazy val _title: StringExpression = new StringExpressionImpl(this, "_title")
  def title: StringExpression = _title
  
  private[this] lazy val _owner: ObjectExpression[User] = new ObjectExpressionImpl[User](this, "_owner")
  def owner: ObjectExpression[User] = _owner
  
  private[this] lazy val _content: StringExpression = new StringExpressionImpl(this, "_content")
  def content: StringExpression = _content
}

object QFile {
  def apply(parent: PersistableExpression[File], name: String, depth: Int): QFile = {
    new PersistableExpressionImpl[File](parent, name) with QFile
  }
  
  def apply(cls: Class[File], name: String, exprType: ExpressionType): QFile = {
    new PersistableExpressionImpl[File](cls, name, exprType) with QFile
  }
  
  private[this] lazy val jdoCandidate: QFile = candidate("this")
  
  def candidate(name: String): QFile = QFile(null, name, 5)
  
  def candidate(): QFile = jdoCandidate
  
  def parameter(name: String): QFile = QFile(classOf[File], name, ExpressionType.PARAMETER)
  
  def variable(name: String): QFile = QFile(classOf[File], name, ExpressionType.VARIABLE)
}
