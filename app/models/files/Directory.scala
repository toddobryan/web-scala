package models.files

import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import scalajdo._
import models.auth._
import models.files._
import scala.collection.JavaConverters._
import util.UsesDataStore
import util.TestableDirectory

@PersistenceCapable(detachable = "true")
class Directory extends Item with TestableDirectory with UsesDataStore {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  private[this] var _title: String = _
  @Persistent(defaultFetchGroup = "true")
  private[this] var _owner: User = _
  private[this] var _parentId: Long = _

  def this(title: String, owner: User, parentId: Long) {
    this()
    _title = title
    _owner = owner
    _parentId = parentId
  }

  // F.O. => File Organizer
  def asHtmlFO: scala.xml.Elem = {
    <li class={ "folder-fo" }>
      <div class="folder-name">{ title }</div>
    </li>
  }

  def asHtmlFO(pathToDir: String): scala.xml.Elem = {
    <li class={ "folder-fo" }>
      <a href={ "/fileManager/" + pathToDir + { if (pathToDir == "") "" else "/" } + title }>
        <div class="folder-name">{ title }</div>
      </a>
    </li>
  }

  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem = {
    <li class={ "folder-fo" }>
      <a href={ "/myClasses/" + blockString + "/" + studentString + "/" + { if (pathToDir == "") "" else "/" } + title }>
        <div class="folder-name">{ title }</div>
      </a>
    </li>
  }
}

object Directory extends UsesDataStore {
  def getById(id: Long): Option[Directory] = {
    val cand = QDirectory.candidate
    dataStore.pm.query[Directory].filter(cand.id.eq(id)).executeOption
  }
  
  // TODO: Need to fix this
  def getItems(directory: Directory): List[Item] = Nil
  
  // TODO: And this
  def getItem(pathToFile: List[String]): Option[Item] = None

  def getByOwner(owner: User): List[Directory] = {
    val cand = QDirectory.candidate
    dataStore.pm.query[Directory].filter(cand.owner.eq(owner)).executeList
  }
  
  def getUserRoot(user: User): Directory = {
    val cand = QDirectory.candidate
    dataStore.pm.query[Directory].filter(cand.owner.eq(user).and(cand.parentId.eq(0.toLong))).executeOption() match {
      case Some(root) => root
      case None => {
        val root = new Directory("Home", user, 0)
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
  
  private[this] lazy val _parentId: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_parentId")
  def parentId: NumericExpression[Long] = _parentId
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