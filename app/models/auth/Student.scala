package models.auth

import scala.collection.mutable
import scala.collection.JavaConverters._
import javax.jdo.annotations._
import models.files._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import util.UsesDataStore

@PersistenceCapable(detachable="true")
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
class Student extends User {
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
  }
  
  override def toString = username
}

object Student extends UsesDataStore {
  def getByUsername(username: String): Option[Student] = {
    val cand = QUser.candidate
    dataStore.pm.query[Student].filter(cand.username.eq(username)).executeOption()
  }
}

trait QStudent extends PersistableExpression[Student] {
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

object QStudent {
  def apply(parent: PersistableExpression[Student], name: String, depth: Int): QStudent = {
    new PersistableExpressionImpl[Student](parent, name) with QStudent
  }
  
  def apply(cls: Class[Student], name: String, exprType: ExpressionType): QStudent = {
    new PersistableExpressionImpl[Student](cls, name, exprType) with QStudent
  }
  
  private[this] lazy val jdoCandidate: QStudent = candidate("this")
  
  def candidate(name: String): QStudent = QStudent(null, name, 5)
  
  def candidate(): QStudent = jdoCandidate

  def parameter(name: String): QStudent = QStudent(classOf[Student], name, ExpressionType.PARAMETER)
  
  def variable(name: String): QStudent = QStudent(classOf[Student], name, ExpressionType.VARIABLE)

}


