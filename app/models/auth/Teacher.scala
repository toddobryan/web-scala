package models.auth

import scala.collection.mutable
import scala.collection.JavaConverters._
import javax.jdo.annotations._
import models.files._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import scalajdo.DataStore

@PersistenceCapable(detachable="true")
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
class Teacher extends User {
  def this(username: String, first: String = null, last: String = null, isActive: Boolean = true, isSuperUser: Boolean = false,
      dateJoined: => DateTime = DateTime.now(), lastLogin: DateTime = null, email: String = null, password: String = null) {
    this()
    username_=(username)
    first_=(first)
    last_=(last)
    isActive_=(isActive)
    isSuperUser_=(isSuperUser)
    dateJoined_=(dateJoined)
    // I modified this from the user constructor because it used private access to lastLogin
    lastLogin_=(lastLogin)
    email_=(email)
    setPassword(password)
    permissions_=(mutable.Set[Permission]())
    root_=(new Directory("Home", this, Nil))
  }
  
  def displayName = if(last == null) username else last.get
}

object Teacher {
  def getByUsername(username: String): Option[Teacher] = {
    val cand = QTeacher.candidate
    DataStore.pm.query[Teacher].filter(cand.username.eq(username)).executeOption()
  }
}

trait QTeacher extends PersistableExpression[Teacher] {
  private[this] lazy val _id: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_id")
  def id: NumericExpression[Long] = _id

  private[this] lazy val _username: StringExpression = new StringExpressionImpl(this, "_username")
  def username: StringExpression = _username
  
  private[this] lazy val _first: StringExpression = new StringExpressionImpl(this, "_first")
  def first: StringExpression = _first
  
  private[this] lazy val _last: StringExpression = new StringExpressionImpl(this, "_last")
  def last: StringExpression = _last
    
  private[this] lazy val _email: StringExpression = new StringExpressionImpl(this, "_email")
  def email: StringExpression = _email
}

object QTeacher {
  def apply(parent: PersistableExpression[Teacher], name: String, depth: Int): QTeacher = {
    new PersistableExpressionImpl[Teacher](parent, name) with QTeacher
  }
  
  def apply(cls: Class[Teacher], name: String, exprType: ExpressionType): QTeacher = {
    new PersistableExpressionImpl[Teacher](cls, name, exprType) with QTeacher
  }
  
  private[this] lazy val jdoCandidate: QTeacher = candidate("this")
  
  def candidate(name: String): QTeacher = QTeacher(null, name, 5)
  
  def candidate(): QTeacher = jdoCandidate

  def parameter(name: String): QTeacher = QTeacher(classOf[Teacher], name, ExpressionType.PARAMETER)
  
  def variable(name: String): QTeacher = QTeacher(classOf[Teacher], name, ExpressionType.VARIABLE)

}


