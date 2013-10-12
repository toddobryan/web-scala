package models.auth

import scala.collection.mutable
import scala.collection.JavaConverters._
import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import models.files._
import scalajdo.DataStore
import util.UsesDataStore

@PersistenceCapable(detachable="true")
class Block {
  @PrimaryKey
  @Persistent(valueStrategy=IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  def id: Long = _id
  
  @Unique
  @Column(allowsNull="false")
  private[this] var _name: String = _
  def name: String = _name
  def name_=(theName: String) { _name = theName }
 
  @Persistent(defaultFetchGroup= "true")
  private[this] var _teacher: Teacher = _
  def teacher: Teacher = _teacher
  def teacher_=(theTeacher: Teacher) {_teacher = theTeacher}
  
  private[this] var _students: java.util.Set[Student] = _
  def students: List[Student] = _students.asScala.toList
  def students_=(theStudents: List[Student]) = {
    val mutableStudents = mutable.Set(theStudents.toList:_*)
    _students = mutableStudents.asJava
  }
  
  private[this] var _allBlocks = true
  
  def this(name: String, teacher: Teacher, students: List[Student] = Nil) = {
    this()
    name_=(name)
    teacher_=(teacher)
    students_=(students)
  }
  
  def addStudent(s: Student) = students_=(s :: students)

  def asHtmlTeacher: scala.xml.Elem = {
    <tr><td><a href={"/myClasses/"+name}>{name}</a></td></tr>
  }
  def asHtmlStudent: scala.xml.Elem = {
    <tr><td><a href={"/myClasses/"+name}>{name}</a></td><td>{teacher.displayName}</td></tr>
  }
  
  override def toString = "Block(" + name + ", " + students + ")"
}

object Block extends UsesDataStore {
  def getByTeacher(t: Teacher): List[Block] = {
    val cand = QBlock.candidate
    dataStore.pm.query[Block].filter(cand.teacher.eq(t)).executeList
  }
  
  def getByStudent(s: Student): List[Block] = {
    val cand = QBlock.candidate
    val all = dataStore.pm.query[Block].filter(cand.allBlocks.eq(true)).executeList
    all.filter(_.students.exists(_.id == s.id))
  }
  
  def getAll: List[Block] = {
    val cand = QBlock.candidate
    dataStore.pm.query[Block].filter(cand.allBlocks.eq(true)).executeList
  }
  
  def getByName(name: String): Option[Block] = {
    val cand = QBlock.candidate
    dataStore.pm.query[Block].filter(cand.name.eq(name)).executeOption
  }
}

trait QBlock extends PersistableExpression[Block] {
  private[this] lazy val _id: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_id")
  def id: NumericExpression[Long] = _id
  
  private[this] lazy val _name: StringExpression = new StringExpressionImpl(this, "_name")
  def name: StringExpression = _name
  
  private[this] lazy val _teacher: ObjectExpression[Teacher] = new ObjectExpressionImpl[Teacher](this, "_teacher")
  def teacher: ObjectExpression[Teacher] = _teacher
  
  private[this] lazy val _students: CollectionExpression[java.util.Set[Student], Student] = 
    new CollectionExpressionImpl[java.util.Set[Student], Student](this, "_students")
  def students: CollectionExpression[java.util.Set[Student], Student] = _students
  
  private[this] lazy val _allBlocks: BooleanExpression = new BooleanExpressionImpl(this, "_allBlocks")
  def allBlocks: BooleanExpression = _allBlocks
}

object QBlock {
  def apply(parent: PersistableExpression[Block], name: String, depth: Int): QBlock = {
    new PersistableExpressionImpl[Block](parent, name) with QBlock
  }
  
  def apply(cls: Class[Block], name: String, exprType: ExpressionType): QBlock = {
    new PersistableExpressionImpl[Block](cls, name, exprType) with QBlock
  }
  
  private[this] lazy val jdoCandidate: QBlock = candidate("this")
  
  def candidate(name: String): QBlock = QBlock(null, name, 5)
  
  def candidate(): QBlock = jdoCandidate
  
  def parameter(name: String): QBlock = QBlock(classOf[Block], name, ExpressionType.PARAMETER)
  
  def variable(name: String): QBlock = QBlock(classOf[Block], name, ExpressionType.VARIABLE)
}
