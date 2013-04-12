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
      <form class="in-line">
		<button method="post" action={"/deleteFile/" + id}>
			Delete?
		</button>
	  </form>
    </li>
  }
  
  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem = {
     <li class={"file-fo"}>
       <a href={"/myClasses/" + blockString + "/" + studentString + "/" + {if(pathToDir == "") "" else "/"} + title}>
         <div class="file-title span6">{title}</div>
		 <div class="file-modified span2">{timeString}</div>
       </a>
     </li>
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
    val start = HtmlRepl.out.getBuffer().length()
    HtmlRepl.repl.interpret(content) match {
      case IntpResults.Success => {
        println(assignment.testCode)
        val next = HtmlRepl.out.getBuffer.length
        HtmlRepl.repl.interpret(assignment.testCode) match {
          case IntpResults.Success => {
            val last = HtmlRepl.out.getBuffer.length()
            HtmlRepl.repl.interpret(HtmlRepl.webscalaTester) match {
              case IntpResults.Success => {
                val theTests = HtmlRepl.repl.valueOfTerm(HtmlRepl.repl.mostRecentVar)
                if(HtmlRepl.isCorrectType(theTests)) {
                  renderTestResults(HtmlRepl.unwrapTests(theTests))
                } else {
                  renderTestResults(List(("This error shouldn't happen.", false)))
                }
              }
              case _ => {
                renderTestResults(List(("The following error was encountered while preparing the test results:" +
                						HtmlRepl.out.getBuffer.substring(last) +
                						"The class's teacher may not have set up the tests correctly.", false)))
              }
            }
          }
          case _ => {
            renderTestResults(List(("The following error was encountered while compiling the tests for the assignment:" +
            						HtmlRepl.out.getBuffer.substring(next) +
            						"This could be an error from your teacher's tests, or you may have not defined a required function.", false)))
          }
        }
      }
      case _ => {
        renderTestResults(List(("The following error was encountered while compiling your file:" +
    		  							 HtmlRepl.out.getBuffer.substring(start), false)))
      }
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
	
	def fileSidebar(files: List[File]): scala.xml.Elem = {
	  <div id="files">
	  {for(file <- files) yield {
	    <li>
            <small>
		    <a href={"/file/" + file.id}>
		    {file.title}
	    	<br />
            <div class="muted">{file.timeString}</div>
            </a>
            </small>
	    </li>
	  }}
	  </div>
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
