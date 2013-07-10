package controllers

import scala.tools.nsc.interpreter.{ Results => IntpResults }
import scala.tools.nsc.interpreter.IR.{ Result => IntpResult}
import play.api._
import play.api.mvc._
import webscala._ 
import scalajdo._
import models.files._
import models.auth._
import models.auth.VisitAction
import models.auth.Authenticated
import org.joda.time._
import forms._
import forms.fields._
import forms.validators._
import util.ControllerHelpers._

object Assignments extends Controller {
  //lazy val repl = new HtmlRepl()
  
  def findStudentFile(bString: String, sString: String, dir: Directory, title: List[String])
                     (implicit req: VisitRequest[AnyContent]) = {
    val path = title.mkString("/")
    if(path == "") Okay(views.html.webscala.viewStudentFiles(bString, sString, dir, path))
    val maybeItem = dir.findItem(title)
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
        case Some(_) => ValidationError(List("An assignment with this name already exists."))
      }
    }
  }
  
  def newAssignment(block: String) = VisitAction { implicit req =>
    asTeacher { t =>
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == block)
      withBlock { b =>
        formHandle(form = NewAssignmentForm(b), title = "New Assignment") { vb =>
          val assignName = vb.valueOf(NewAssignmentForm(b).assignmentName).trim
          val startCode = "/* Insert Code Here */"
          val testCode = File.defaultTestCode(assignName)
          val assignment = new Assignment(assignName, b, startCode, testCode, testCode)
          DataStore.pm.makePersistent(assignment)
          Redirect(routes.Assignments.editAssignment(b.name, assignName))
        }
      }
    }
  }
  
  def editAssignment(block: String, assignment: String) = VisitAction {implicit req =>
    asTeacher { t =>
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == block)
      withBlock { b => 
        implicit val maybeAssign = Assignment.getBlockAssignments(b)find(_.title == assignment)
        withAssignment { a => 
          if(req.method == "GET") {
            Okay(views.html.webscala.editAssignment(b, a))
          } else {
            val startCode = getParameter(req, "start")
            val testCode = getParameter(req, "test")
            a.starterCode_=(startCode)
            a.testCode_=(testCode)
            DataStore.pm.makePersistent(b)
            Redirect(routes.Classes.findMyBlock(b.name))
          }
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
         implicit val maybeClassDir = root.content.find(_.title == block)
         withDir {d => 
           val alreadyAssigned = d.content.find(_.title == assignment)
           alreadyAssigned match {
             case None => {
               d.addFile(new File(a.title, u, starterCode))
               DataStore.pm.makePersistent(u)
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
          implicit val classRoot = Directory.getUserRoot(s).findItem(block)
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
          implicit val userRoot = Directory.getUserRoot(s).findItem(block)
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
          implicit val userRoot = Directory.getUserRoot(s).findItem(block)
          withDir { dir => 
            val path = titles.split("/").toList
            val maybeFile = dir.findItem(path)
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