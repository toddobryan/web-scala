package models.files

import scala.collection.mutable
import scala.collection.JavaConverters._
import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import scalajdo.DataStore
import models.auth.{Block, QBlock}
import util.UsesDataStore

@PersistenceCapable(detachable="true")
class Assignment {
  def this(title: String, block: Block, starterCode: String, testCode: String, offTestCode: String) {
    this()
    title_=(title)
    block_=(block)
    starterCode_=(starterCode)
    testCode_=(testCode)
    offTestCode_=(offTestCode)
  }

  @PrimaryKey
  @Persistent(valueStrategy=IdGeneratorStrategy.INCREMENT)
  private[this] var _id: Long = _
  def id: Long = _id
  
  private[this] var _title: String = _
  def title: String = _title
  def title_=(theTitle: String) = {_title = theTitle}
  
  @Column(length=1048576) // 1MB
  private[this] var _starterCode: String = _
  def starterCode: String = _starterCode
  def starterCode_=(theStarter: String) = {_starterCode = theStarter}
  
  @Column(length=1048576) // 1MB
  private[this] var _testCode: String = _
  def testCode: String = _testCode
  def testCode_=(theTest: String) = {_testCode = theTest}
  
  @Column(length=1048576) // 1MB
  private[this] var _offTestCode: String = _
  def offTestCode: String = _offTestCode
  def offTestCode_=(theOffTest: String) = {_offTestCode = theOffTest}
  
  @Persistent(defaultFetchGroup= "true")
  private[this] var _block: Block = _
  def block: Block = _block
  def block_=(theBlock: Block) = {_block = theBlock}
}

object Assignment extends UsesDataStore {
  def getBlockAssignments(block: Block): List[Assignment] = {
    val cand = QAssignment.candidate
    dataStore.pm.query[Assignment].filter(cand.block.eq(block)).executeList
  }
}

trait QAssignment extends PersistableExpression[Assignment] {
  private[this] lazy val _id: NumericExpression[Long] = new NumericExpressionImpl[Long](this, "_id")
  def id: NumericExpression[Long] = _id
  
  private[this] lazy val _title: StringExpression = new StringExpressionImpl(this, "_title")
  def title: StringExpression = _title
  
  private[this] lazy val _starterCode: StringExpression = new StringExpressionImpl(this, "_starterCode")
  def starterCode: StringExpression = _starterCode
  
  private[this] lazy val _testCode: StringExpression = new StringExpressionImpl(this, "_testCode")
  def testCode: StringExpression = _testCode
  
  private[this] lazy val _block: ObjectExpression[Block] = new ObjectExpressionImpl(this, "_block")
  def block: ObjectExpression[Block] = _block
}

object QAssignment {
  def apply(parent: PersistableExpression[Assignment], name: String, depth: Int): QAssignment = {
    new PersistableExpressionImpl[Assignment](parent, name) with QAssignment
  }
  
  def apply(cls: Class[Assignment], name: String, exprType: ExpressionType): QAssignment = {
    new PersistableExpressionImpl[Assignment](cls, name, exprType) with QAssignment
  }
  
  private[this] lazy val jdoCandidate: QAssignment = candidate("this")
  
  def candidate(name: String): QAssignment = QAssignment(null, name, 5)
  
  def candidate(): QAssignment = jdoCandidate
  
  def parameter(name: String): QAssignment = QAssignment(classOf[Assignment], name, ExpressionType.PARAMETER)
  
  def variable(name: String): QAssignment = QAssignment(classOf[Assignment], name, ExpressionType.VARIABLE)
}