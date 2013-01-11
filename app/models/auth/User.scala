package models.auth

import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._

@PersistenceCapable(detachable="true")
class User {
  @PrimaryKey
  @Persistent(valueStrategy=IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  
  @Unique
  @Column(allowsNull="false")
  private[this] var _username: String = _

  private[this] var _first: String = _
  private[this] var _last: String = _
  //TODO: need better types for email and password
  private[this] var _email: String = _
  private[this] var _password: String = _
  
  def this(username: String, first: String = null, last: String = null, 
      email: String = null, password: String = null) {
    this()
    _username = username
    _first = first
    _last = last
    _email = email
    _password = password
  }
  
  def id: Long = _id
  
  def username: String = _username
  def username_=(theUsername: String) { _username = theUsername }
  
  def first: Option[String] = Option(_first)
  def first_=(theFirst: Option[String]) { _first = theFirst.getOrElse(null) }
  def first_=(theFirst: String) { _first = theFirst }
  
  def last: Option[String] = Option(_last)
  def last_=(theLast: Option[String]) { _last = theLast.getOrElse(null) }
  def last_=(theLast: String) { _last = theLast }
  
  def email: Option[String] = Option(_email)
  def email_=(theEmail: Option[String]) { _email = theEmail.getOrElse(null) }
  def email_=(theEmail: String) { _email = theEmail }
  
  def password: Option[String] = Option(_password)
  def password_=(thePassword: Option[String]) { _password = thePassword.getOrElse(null) }
  def password_=(thePassword: String) { _password = thePassword }
}

object User {
  
}

trait QUser extends PersistableExpression[User] {
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
  
  private[this] lazy val _password: StringExpression = new StringExpressionImpl(this, "_password")
  def password: StringExpression = _password
}

object QUser {
  def apply(parent: PersistableExpression[User], name: String, depth: Int): QUser = {
    new PersistableExpressionImpl[User](parent, name) with QUser
  }
  
  def apply(cls: Class[User], name: String, exprType: ExpressionType): QUser = {
    new PersistableExpressionImpl[User](cls, name, exprType) with QUser
  }
  
  private[this] lazy val jdoCandidate: QUser = candidate("this")
  
  def candidate(name: String): QUser = QUser(null, name, 5)
  
  def candidate(): QUser = jdoCandidate

  def parameter(name: String): QUser = QUser(classOf[User], name, ExpressionType.PARAMETER)
  
  def variable(name: String): QUser = QUser(classOf[User], name, ExpressionType.VARIABLE)

}


