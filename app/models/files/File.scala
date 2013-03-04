package models.files

import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import scalajdo._
import models.auth._

@PersistenceCapable(detachable="true")
class File {
  @PrimaryKey
  @Persistent(valueStrategy=IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  private[this] var _title: String = _
  @Persistent(defaultFetchGroup = "true")
  private[this] var _owner: User = _
  @Column(length=1048576) // 1MB
  private[this] var _content: String = _
  
  def this(title: String, owner: User, content: String) {
    this()
    _title = title
    _owner = owner
    _content = content
  }
  
  def id: Long = _id
  
  def title: String = _title
  def title_=(theTitle: String) = (_title = theTitle)
  
  def owner: User = _owner
  def owner_=(theOwner: User) = (_owner = theOwner)
  
  def content: String = _content
  def content_=(theContent: String) = (_content = theContent)
  
  override def toString = {
    "%s -- %s".format(title, owner)
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
