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


object WebScala extends Controller {
  //lazy val repl = new HtmlRepl()

  def ide = Authenticated { implicit req =>
    Okay(views.html.webscala.ide())
  }
  
  case class NewFileForm(val dir: Directory) extends Form {
    val fileName = new TextField("fileName")
    val dirOrFile = new ChoiceField("Type", List(("File", "file"), ("Directory", "dir")))
    def fields = List(fileName, dirOrFile)
    
    override def validate(vb: ValidBinding): ValidationError = {
      dir.content.filter(_.title == vb.valueOf(NewFileForm(dir).fileName).trim) match {
        case Nil => ValidationError(Nil)
        case file :: _ => ValidationError(List("File with this name already exists."))
      }
    }
  }
  
  def newFileHome() = newFile("")
  
  def newFile(titles: String) = VisitAction { implicit req =>
    val titlesList = if(titles == "") Nil else titles.split("/").toList
    asUser { user => 
      implicit val maybeItem = user.root.findItem(titlesList)
      withDir { dir =>
        formHandle(form = NewFileForm(dir), title = "Add File") { vb =>
          val name = vb.valueOf(NewFileForm(dir).fileName).trim()
          val dirOrFile = vb.valueOf(NewFileForm(dir).dirOrFile)
          if(dirOrFile == "dir") {
            val newDir = new Directory(name, user, Nil, 2)
            dir.addDirectory(newDir)
          } else {
            val file = new File(name, user, "/* Enter Code Here */", Some(DateTime.now))
            dir.addFile(file)
          }
          if(titles == "") {
            Redirect("/fileManager").flashing(("success" -> "Item Created"))
          } else {
            Redirect("/fileManager/" + titles).flashing(("success" -> "Item Created"))
          }
        }
      }
    }
  }
  
  def fileManagerHome() = VisitAction { implicit req =>
    asUser { user => Okay(views.html.webscala.displayFiles(user.root, "")) }
  }
  
 def fileManager(titles: String) = VisitAction { implicit req =>
    asUser { user =>
      val directory = user.root
      val titlesList = titles.split("/").toList
      findFile(directory, titlesList)
    }
 }

  def findFile(dir: Directory, title: List[String])(implicit req: VisitRequest[AnyContent]) = {
    val path = title.mkString("/")
    val maybeItem = dir.findItem(title)
    dirOrFile(maybeItem) 
      { d: Directory => Okay(views.html.webscala.displayFiles(d, path)) }
      { f: File => Okay(views.html.webscala.newGetFile(f, path)) }
  }
  
  def findStudentFile(bString: String, sString: String, dir: Directory, title: List[String])
                     (implicit req: VisitRequest[AnyContent]) = {
    val path = title.mkString("/")
    if(path == "") Okay(views.html.webscala.viewStudentFiles(bString, sString, dir, path))
    val maybeItem = dir.findItem(title)
    dirOrFile(maybeItem)
      { d: Directory => Okay(views.html.webscala.viewStudentFiles(bString, sString, d, path)) }
      { f: File => Okay(views.html.webscala.viewStudentFile(bString, sString, f, path)) }
  }
  
  object NewBlockForm extends Form {
    def blockName = new TextField("blockName")
    
    def fields = List(blockName)
    
    override def validate(vb: ValidBinding): ValidationError = {
      def blockName = vb.valueOf(NewBlockForm.blockName).trim
      Block.getByName(blockName) match {
        case None => ValidationError(Nil)
        case Some(_) => ValidationError(List("A block with this name already exists."))
      }
    }
  }
  
  def newBlock() = VisitAction {implicit req =>
    asTeacher { t => 
      formHandle( form = NewBlockForm, title = "Add Block") { 
        vb =>
        val blockName = vb.valueOf(NewBlockForm.blockName).trim
        val newBlock = new Block(blockName, t)
        DataStore.pm.makePersistent(newBlock)
        Redirect(routes.WebScala.myBlocks()).flashing(("success" -> "New Class Created"))
      }
    }
  }
  
  def myBlocks() = VisitAction { implicit req =>
    teacherOrStudent
      {t => Okay(views.html.webscala.displayBlocksTeacher(Block.getByTeacher(t)))}
      {s => Okay(views.html.webscala.displayBlocksStudent(Block.getByStudent(s)))}
  }
  
  def findMyBlock(name: String) = VisitAction {implicit req =>
    val tAction: ToResult[Teacher] = { t => 
      val maybeBlock = Block.getByTeacher(t).find(_.name == name)
      withBlock(maybeBlock) { b: Block => Okay(views.html.webscala.displayBlockTeacher(b)) }
    }
    val sAction: ToResult[Student] = { s => 
      val maybeBlock = Block.getByStudent(s).find(_.name == name)
      withBlock(maybeBlock) { b: Block => Okay(views.html.webscala.displayBlockTeacher(b)) }
    }
    teacherOrStudent { tAction } { sAction}
  }
  
  object JoinBlockForm extends Form {
    def blockName = new TextField("blockName")
    def joinCode =  new TextField("joinCode")
    
    def fields = List(blockName, joinCode)
    
    override def validate(vb: ValidBinding): ValidationError = {
      Block.getByName(vb.valueOf(JoinBlockForm.blockName)) match {
        case Some(b: Block) => {
          if(b.id.toString == vb.valueOf(joinCode)) {
            new ValidationError(Nil)
          } else {
            new ValidationError(List("Class and Join Code did not match."))
          }
        }
        case None => {
          new ValidationError(List("Class could not be found."))
        }
      }
    }
  }
  
  def joinBlock() = VisitAction { implicit req =>
    asStudent { s => 
      formHandle( form = JoinBlockForm, title = "Join Block") { vb => 
        implicit val maybeBlock = Block.getByName(vb.valueOf(JoinBlockForm.blockName)) 
        withBlock { b =>
          if(b.students.contains(s))  Redirect(routes.WebScala.joinBlock()).flashing(("error") -> "You are already a member of this class.")
          else {
            b.addStudent(s)
            DataStore.pm.makePersistent(b)
            s.root.addDirectory(new Directory(b.name, s, Nil))
            DataStore.pm.makePersistent(s)
            Redirect(routes.WebScala.myBlocks).flashing(("success") -> ("You have been added to this class, and a class directory" 
                                                                         + " has been added to your home folder."))
          }
        }
      }
    }
  }
  
  case class NewAssignmentForm(val block: Block) extends Form {
    def assignmentName = new TextField("assignmentName")
    
    def fields = List(assignmentName)
    
    override def validate(vb: ValidBinding): ValidationError = {
      def assignmentName = vb.valueOf(NewAssignmentForm(block).assignmentName).trim
      block.assignments.find(_.title == assignmentName) match {
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
          val initialTestCode =
"""// You can test student code by testing functionality.
// Then create a value called myTests, which is a list of
// (String, Boolean, String) triples, where the first
// string is the title for a test, the boolean is the result
// of the test (true=success, false=failure), and the
// last string is a diagnostic for the student."""
          val assignment = new Assignment(assignName, "", initialTestCode)
          b.addAssignemnt(assignment)
          DataStore.pm.makePersistent(b)
          Redirect(routes.WebScala.editAssignment(b.name, assignName))
        }
      }
    }
  }
  
  def editAssignment(block: String, assignment: String) = VisitAction {implicit req =>
    asTeacher { t =>
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == block)
      withBlock { b => 
        implicit val maybeAssign = b.assignments.find(_.title == assignment)
        withAssignment { a => 
          if(req.method == "GET") {
            Okay(views.html.webscala.editAssignment(b, a))
          } else {
            val startCode = getParameter(req, "start")
            val testCode = getParameter(req, "test")
            a.starterCode_=(startCode)
            a.testCode_=(testCode)
            DataStore.pm.makePersistent(b)
            Redirect(routes.WebScala.findMyBlock(b.name))
          }
        }
      }
    }
  }
  
  def startAssignment(block: String, assignment: String) = VisitAction { implicit req =>
    asUser { u =>
     withBlock(Block.getByName(block)) { b =>
       implicit val maybeAssign = b.assignments.find(_.title == assignment)
       withAssignment { a => 
         def starterCode = a.starterCode
         implicit val maybeClassDir = u.root.content.find(_.title == block)
         withDir {d => 
           val alreadyAssigned = d.content.find(_.title == assignment)
           alreadyAssigned match {
             case None => {
               d.addFile(new File(a.title, u, starterCode))
               DataStore.pm.makePersistent(u)
               Redirect(routes.WebScala.fileManager(block + "/" + assignment))
             }
             case _ => Redirect(routes.WebScala.fileManager(block + "/" + assignment))
           }
         }
       }
     }
    } 
  }
  
  def submitFile(block: String, assignment: String) = VisitAction { implicit req => 
    asUser { u => 
      withBlock(Block.getByName(block)) { b => 
        implicit val maybeAssign = b.assignments.find(_.title == assignment)
        withAssignment { a =>
          implicit val matchingFile = u.root.findItem(List(block, assignment))
          withFile { f => Okay(views.html.webscala.showTestResults(a, f)) }
        }
      }
    }
  }
  
  def getStudentFiles(block: String, student: String) = VisitAction {implicit req =>
    asTeacher { t => 
      val blocks = Block.getByTeacher(t)
      implicit val maybeBlock = blocks.find(_.name == block)
      withBlock { b => 
        val maybeStudent = b.students.find(_.username == student)
        withObject(maybeStudent, "No such student exists.") { s =>
          val classRoot = s.root.findItem(block)
          withDir(classRoot){ dir => findStudentFile(block, student, dir, Nil) }
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
          implicit val userRoot = s.root.findItem(block)
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
          implicit val userRoot = s.root.findItem(block)
          withDir { dir => 
            val path = titles.split("/").toList
            val maybeFile = dir.findItem(path)
            val maybeAssign = b.assignments.find(_.title == path.head)
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