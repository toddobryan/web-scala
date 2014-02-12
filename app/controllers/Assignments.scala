package controllers

import scala.tools.nsc.interpreter.{ Results => IntpResults }
import scala.tools.nsc.interpreter.IR.{ Result => IntpResult}
import play.api._
import play.api.mvc._
import webscala._
import scalajdo.DataStore
import models.files._
import models.auth._
import models.auth.VisitAction
import models.auth.Authenticated
import org.joda.time._
import org.dupontmanual.forms._
import org.dupontmanual.forms.fields._
import org.dupontmanual.forms.validators._
import util.ControllerHelpers._
import util.UsesDataStore
import models.files.Directory

object Assignments extends Controller with UsesDataStore {
  //lazy val repl = new HtmlRepl()
  
  def findStudentFile(bString: String, sString: String, dir: Directory, title: List[String])
                     (implicit req: VisitRequest[AnyContent]) = {
    val path = title.mkString("/")
    if(path == "") Okay(views.html.webscala.viewStudentFiles(bString, sString, dir, path))
    val maybeItem = /*TODO: Fix*/Directory.getItem(title)
    dirOrFile(maybeItem)
      { d: Directory => Okay(views.html.webscala.viewStudentFiles(bString, sString, d, path)) }
      { f: File => Okay(views.html.webscala.viewStudentFile(bString, sString, f, path)) }
  }
  
  case class NewAssignmentForm(val block: Block) extends Form {
    def assignmentName = new TextField("assignmentName")
    
    def fields = List(assignmentName)
    
    override def validate(vb: ValidBinding): ValidationError = {
      def assignmentName = vb.valueOf(NewAssignmentForm(block).assignmentName).trim
      Assignment.getBlockAssignments(block).find(_.title == assignmentName) match {
        case None => ValidationError(Nil)
        case Some(_) => ValidationError("An assignment with this name already exists.")
      }
    }
  }
  
  def newAssignmentActions(b: Block)(implicit req: VRequest) = {
    formHandle(form = NewAssignmentForm(b), title = "New Assignment") {
      vb: ValidBinding => 
        val assignName = vb.valueOf(NewAssignmentForm(b).assignmentName).trim
        val startCode = "/* Insert Code Here */"
        val testCode = File.defaultTestCode(assignName)
        val assignment = new Assignment(assignName, b, startCode, testCode, testCode)
        dataStore.pm.makePersistent(assignment)
        Redirect(routes.Assignments.editAssignment(b.name, assignName))
    }
  }
  
  def newAssignment(block: String) = VisitAction { implicit req =>
    asTeacher { t =>
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == block)
      withBlock { b => newAssignmentActions(b)._1 }
    }
  }
  
  def newAssignmentP(block: String) = VisitAction { implicit req =>
    asTeacher { t =>
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == block)
      withBlock { b => newAssignmentActions(b)._2}
    }
  }
  
  def editAssignment(block: String, assignment: String) = VisitAction {implicit req =>
    asTeacher { t =>
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == block)
      withBlock { b => 
        implicit val maybeAssign = Assignment.getBlockAssignments(b)find(_.title == assignment)
        withAssignment { a => Okay(views.html.webscala.editAssignment(b, a)) }
      }
    }
  }
  
  def editAssignmentP(block: String, assignment: String) = VisitAction { implicit req =>
    asTeacher { t =>
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == block)
      withBlock { b => 
        implicit val maybeAssign = Assignment.getBlockAssignments(b)find(_.title == assignment)
        withAssignment { a => 
          val startCode = getParameter(req, "start")
          val testCode = getParameter(req, "test")
          a.starterCode_=(startCode)
          a.testCode_=(testCode)
          dataStore.pm.makePersistent(b)
          Redirect(routes.Classes.findMyBlock(b.name))
        }
      }
    }
  }
  
  def startAssignment(block: String, assignment: String) = VisitAction { implicit req =>
    asUser { u =>
     withBlock(Block.getByName(block)) { b =>
       implicit val maybeAssign = Assignment.getBlockAssignments(b).find(_.title == assignment)
       withAssignment { a => 
         def starterCode = a.starterCode
         val root = Directory.getUserRoot(u)
         implicit val maybeClassDir = Directory.getItems(root).find(_.title == block)
         withDir {d => 
           val alreadyAssigned = Directory.getItems(d).find(_.title == assignment)
           alreadyAssigned match {
             case None => {
               /* TODO: Fix Here d.addFile(new File(a.title, u, starterCode))*/
               dataStore.pm.makePersistent(u)
               Redirect(routes.FileMgmt.fileManager(block + "/" + assignment))
             }
             case _ => Redirect(routes.FileMgmt.fileManager(block + "/" + assignment))
           }
         }
       }
     }
    } 
  }
  
  def getStudentFiles(block: String, student: String) = VisitAction {implicit req =>
    asTeacher { t => 
      val blocks = Block.getByTeacher(t)
      implicit val maybeBlock = blocks.find(_.name == block)
      withBlock { b => 
        implicit val maybeStudent = b.students.find(_.username == student)
        withObject[Student]("No such student exists.") { s =>
          implicit val classRoot = Directory.getItem(List(block))/* TODO: Fix.getUserRoot(s).findItem(block)*/
          withDir { dir => findStudentFile(block, student, dir, Nil) }
        }
      }
    }
  }
  
  def getStudentFiles(block: String, student: String, titles: String) = VisitAction {implicit req =>
    asTeacher { t => 
      val blocks = Block.getByTeacher(t)
      withBlock(blocks.find(_.name == block)) { b =>
        implicit val maybeStudent = b.students.find(_.username == student)
        withObject[Student]("Student with given name not in block.") { s =>
          implicit val userRoot = /* TODO: Fix Here. Directory.getUserRoot(s)*/Directory.getItem(List(block))
          withDir { dir =>
            val path = titles.split("/").toList
            findStudentFile(block, student, dir, path)
          }
        }
      }
    }
  }
  
  def getStudentAssignmentTest(block: String, student: String, titles: String)= VisitAction {implicit req =>
    asTeacher { t => 
      val blocks = Block.getByTeacher(t)
      implicit val maybeBlock = blocks.find(_.name == block)
      withBlock { b =>
        implicit val maybeStudent = b.students.find(_.username == student)
        withObject[Student]("Student with given name not in block.") { s =>
          implicit val userRoot = /*TODO: Fix Directory.getUserRoot(s).*/Directory.getItem(List(block))
          withDir { dir => 
            val path = titles.split("/").toList
            val maybeFile = /* TODO: Fix dir*/Directory.getItem(path)
            val maybeAssign = Assignment.getBlockAssignments(b).find(_.title == path.head)
            (maybeFile, maybeAssign) match {
              case (Some(f: File), Some(a: Assignment)) =>Okay(views.html.webscala.showTestResults(a, f))
              case _ => Redirect(routes.Application.index()).flashing(("error") -> "A file or assignment with given name could not be found.")
            }
          }
        }
      }
    }
  }
}