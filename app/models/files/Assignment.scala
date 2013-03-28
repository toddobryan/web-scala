package models.files

import scala.collection.mutable
import scala.collection.JavaConverters._
import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import scalajdo.DataStore

@PersistenceCapable(detachable="true")
class Assignment {
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
}