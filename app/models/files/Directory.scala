package models.files

import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import org.joda.time.DateTime
import scalajdo._
import models.auth._
import models.files._
import scala.collection.JavaConverters._
import util.UsesDataStore

@PersistenceCapable(detachable = "true")
class Directory extends Item {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  private[this] var _title: String = _
  @Persistent(defaultFetchGroup = "true")
  private[this] var _owner: User = _

  @Element(types = Array(classOf[Item]))
  @Join
  private[this] var _content: java.util.List[Item] = _

  private[this] var _levelsFromRoot: Int = _

  def this(title: String, owner: User, content: List[Item], levelsFromRoot: Int = 0) {
    this()
    _title = title
    _owner = owner
    content_=(content)
    _levelsFromRoot = levelsFromRoot
  }

  def id: Long = _id

  def title: String = _title
  def title_=(theTitle: String) = (_title = theTitle)

  def owner: User = _owner
  def owner_=(theOwner: User) = (_owner = theOwner)

  def content: List[Item] = _content.asScala.toList
  def content_=(theContent: List[Item]) = (_content = theContent.asJava)

  def levelsFromRoot = _levelsFromRoot
  def levelsFromRoot_=(daLevels: Int) = (_levelsFromRoot = daLevels)

  def rootClassString = _levelsFromRoot + "fromRoot"

  // F.O. => File Organizer
  def asHtmlFO: scala.xml.Elem = {
    <li class={ "folder-fo " + rootClassString }>
      <div class="folder-name">{ title }</div>
    </li>
  }

  def asHtmlFO(pathToDir: String): scala.xml.Elem = {
    <li class={ "folder-fo " + rootClassString }>
      <a href={ "/fileManager/" + pathToDir + { if (pathToDir == "") "" else "/" } + title }>
        <div class="folder-name">{ title }</div>
      </a>
    </li>
  }

  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem = {
    <li class={ "folder-fo " + rootClassString }>
      <a href={ "/myClasses/" + blockString + "/" + studentString + "/" + { if (pathToDir == "") "" else "/" } + title }>
        <div class="folder-name">{ title }</div>
      </a>
    </li>
  }

  def addFile(f: File) = (content_=(f :: content))

  def addDirectory(d: Directory) = {
    d.levelsFromRoot_=(levelsFromRoot + 1)
    content_=(d :: content)
  }

  def sortedContent = {
    def sorter(i: Item, j: Item) = (i, j) match {
      case (i: Directory, j: Directory) => i.title < j.title
      case (i: Directory, j: File) => true
      case (i: File, j: Directory) => false
      case (i: File, j: File) => i.title < j.title
    }
    content.sortWith(sorter(_, _))
  }

  def deleteFile(f: File) = (content_=(content.filterNot(_ == f)))

  def deleteDirectory(d: Directory) = (content_=(content.filterNot(_ == d)))

  def findItem(title: String): Option[Item] = {
    content.filter(_.title == title) match {
      case Nil => None
      case x :: xs => Some(x)
    }
  }

  def findItem(titles: List[String]): Option[Item] = {
    def findItemHelper(titles: List[String], currItem: Option[Item]): Option[Item] = titles match {
      case Nil => currItem
      case name1 :: more =>
        currItem match {
          case Some(file: File) => None
          case Some(dir: Directory) => {
            val inDirectory = dir.content.find(_.title == name1)
            findItemHelper(more, inDirectory)
          }
          case _ => None
        }
    }
    findItemHelper(titles, Some(this))
  }
}

object Directory extends UsesDataStore {
  def getById(id: Long): Option[Directory] = {
    val cand = QDirectory.candidate
    dataStore.pm.query[Directory].filter(cand.id.eq(id)).executeOption
  }

  def getByOwner(owner: User): List[Directory] = {
    val cand = QDirectory.candidate
    dataStore.pm.query[Directory].filter(cand.owner.eq(owner)).executeList
  }
  
  def getUserRoot(user: User): Directory = {
    val cand = QDirectory.candidate
    dataStore.pm.query[Directory].filter(cand.owner.eq(user).and(cand.title.eq("Home"))).executeOption() match {
      case Some(root) => root
      case None => {
        val root = new Directory("Home", user, Nil)
        dataStore.pm.makePersistent(root)
        root
      }
    }
  }
}

trait QDirectory extends PersistableExpression[Directory] {
  private[this] lazy val _id: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_id")
  def id: NumericExpression[Long] = _id

  private[this] lazy val _title: StringExpression = new StringExpressionImpl(this, "_title")
  def title: StringExpression = _title

  private[this] lazy val _owner: ObjectExpression[User] = new ObjectExpressionImpl[User](this, "_owner")
  def owner: ObjectExpression[User] = _owner
}

object QDirectory {
  def apply(parent: PersistableExpression[Directory], name: String, depth: Int): QDirectory = {
    new PersistableExpressionImpl[Directory](parent, name) with QDirectory
  }

  def apply(cls: Class[Directory], name: String, exprType: ExpressionType): QDirectory = {
    new PersistableExpressionImpl[Directory](cls, name, exprType) with QDirectory
  }

  private[this] lazy val jdoCandidate: QDirectory = candidate("this")

  def candidate(name: String): QDirectory = QDirectory(null, name, 5)

  def candidate(): QDirectory = jdoCandidate

  def parameter(name: String): QDirectory = QDirectory(classOf[Directory], name, ExpressionType.PARAMETER)

  def variable(name: String): QDirectory = QDirectory(classOf[Directory], name, ExpressionType.VARIABLE)
}