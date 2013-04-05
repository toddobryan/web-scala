package controllers

import scala.tools.nsc.interpreter.{ Results => IntpResults }
import play.api._
import play.api.mvc._
import webscala.HtmlRepl
import scalajdo._
import models.files._
import models.auth._
import models.auth.VisitAction
import models.auth.Authenticated
import org.joda.time._
import forms._
import forms.fields._
import forms.validators._

object WebScala extends Controller {
  //lazy val repl = new HtmlRepl()

  def ide = Authenticated { implicit req =>
    Ok(views.html.webscala.ide())
  }

  def interpret = VisitAction { implicit req =>
    println(req.body.asFormUrlEncoded.getOrElse(Map()))
    val line = req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("line", Nil) match {
      case Nil => ""
      case fst :: rst => fst
    }
    println(line)
    val start = HtmlRepl.out.getBuffer.length
    HtmlRepl.repl.interpret(line) match {
      case IntpResults.Success => Ok("" + HtmlRepl.repl.valueOfTerm(HtmlRepl.repl.mostRecentVar).getOrElse(""))
      case IntpResults.Error => Ok(HtmlRepl.out.getBuffer.substring(start))
      case IntpResults.Incomplete => Ok("We don't do incomplete statements, yet.")
    }
  }
  
  def compile(titles: String) = VisitAction { implicit req =>
    println("Compiling content of files")
    val content = req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("line", Nil) match {
      case Nil => ""
      case fst :: rst => fst
    }
    req.visit.user match {
      case None => false
      case Some(user) => {
        user.root.findItem(titles.split("/").toList) match {
          case None => println("Error in saving file.")
          case Some(_: Directory) => println("This is a directory. What happened?")
          case Some(f: File) => {
            f.content_=(content)
            f.lastModified_=(DateTime.now)
            DataStore.pm.makePersistent(f.owner)
            println("No errors")
          }
        }
      }
    }
    val start = HtmlRepl.out.getBuffer.length
    HtmlRepl.repl.interpret(content) match {
      case IntpResults.Success => Ok("No errors in compiling this file!")
      case _ => Ok(HtmlRepl.out.getBuffer.substring(start))
    }
  }
  
  case class NewFileForm(val dir: Directory) extends Form {
    val fileName = new TextField("fileName")
    val dirOrFile = new ChoiceField("folder", List(("File", "file"),
    										       ("Directory", "dir")))
    def fields = List(fileName, dirOrFile)
    
    override def validate(vb: ValidBinding): ValidationError = {
      dir.content.filter(_.title == vb.valueOf(NewFileForm(dir).fileName).trim) match {
        case Nil => ValidationError(Nil)
        case file :: _ => ValidationError(List("File with this name already exists."))
      }
    }
  }
  
  def newFileHome(): Action[AnyContent] = newFile("")
  
  def newFile(titles: String): Action[AnyContent] = VisitAction { implicit req =>
    val titlesList = if(titles == "") Nil else titles.split("/").toList
    req.visit.user match {
        case None => Redirect(routes.Application.index()).flashing(("error" -> "You must be logged in to create a file"))
        case Some(user) => {
          user.root.findItem(titlesList) match {
            case None => Redirect(routes.WebScala.fileManagerHome).flashing(("error" -> "Directory not found"))
            case Some(_: File) => Redirect(routes.WebScala.fileManagerHome).flashing(("error" -> "Cannot add to a file"))
            case Some(dir: Directory) => {
              if(req.method == "GET") {
                Ok(views.html.webscala.newFile(Binding(NewFileForm(dir))))
              } else {	
                Binding(NewFileForm(dir), req) match {
                  case ib: InvalidBinding => Ok(views.html.webscala.newFile(ib))
                  case vb: ValidBinding => {
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
         }
       }
    }
  }
  
  // this doesn't work lol, haven't updated it.
  def deleteFile(dirId: Long)(id: Long) = VisitAction { implicit req =>
    req.visit.user match {
      case None => Redirect(routes.Application.index()).flashing(("error" -> "You must be logged in to delete a file."))
      case Some(user) => {
        val maybeFile = File.getById(id)
        maybeFile match {
          case None => Redirect(routes.Application.index()).flashing(("error" -> "No such file exists"))
          case Some(f) => {
            DataStore.pm.deletePersistent(f)
            Redirect(routes.WebScala.fileManagerHome()).flashing(("success" -> "File successfully deleted"))
          }
        }
      }
    }  
  }
  
  def fileManagerHome() = VisitAction { implicit req =>
    req.visit.user match {
      case None => Redirect(routes.Application.index()).flashing(("error" -> "You must be logged in to access files"))
      case Some(user) => Ok(views.html.webscala.displayFiles(user.root, ""))
    }
  }
  
 def fileManager(titles: String) = VisitAction { implicit req =>
    req.visit.user match {
      case None => Redirect(routes.Application.index()).flashing(("error" -> "You must be logged in to access files"))
      case Some(user) => {
        val directory = user.root
        val titlesList = titles.split("/").toList
        findFile(directory, titlesList)
      }
    }
  }

  def findFile(dir: Directory, title: List[String])(implicit req: VisitRequest[AnyContent]) = {
    val path = title.mkString("/")
    val urlPath = path + "/"
    dir.findItem(title) match {
      case None => Redirect(routes.Application.index()).flashing(("error" -> "No Such File"))
      case Some(f: File) => Ok(views.html.webscala.getFile(f, path))
      case Some(d: Directory) => Ok(views.html.webscala.displayFiles(d, path))
    }
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
    req.visit.user match {
      case Some(t: Teacher) => {
        if(req.method == "GET") {
          Ok(views.html.webscala.newBlock(Binding(NewBlockForm)))
        } else {
          Binding(NewBlockForm, req) match {
            case ib: InvalidBinding => Ok(views.html.webscala.newBlock(ib))
            case vb: ValidBinding => {
              val blockName = vb.valueOf(NewBlockForm.blockName).trim
              val newBlock = new Block(blockName, t)
              DataStore.pm.makePersistent(newBlock)
              Redirect(routes.Application.index()).flashing(("success" -> "New Class Created"))
            }
          }
        }
      }
      case _ => {
        Redirect(routes.Application.index()).flashing(("error") -> "You must be logged in as a teacher to create a block.")
      }
    }
  }
  
  def myBlocks() = VisitAction {implicit req =>
    req.visit.user match {
      case Some(t: Teacher) => {
        Ok(views.html.webscala.displayBlocksTeacher(Block.getByTeacher(t)))
      }
      case Some(s: Student) => {
        Ok(views.html.webscala.displayBlocksStudent(Block.getByStudent(s)))
      }
      case None => {
        Redirect(routes.Application.index()).flashing(("error") -> "You must be logged in to see your classes.")
      }
    }
  }
  
  def findMyBlock(name: String) = VisitAction {implicit req =>
    req.visit.user match {
      case Some(t: Teacher) => {
        val block = Block.getByTeacher(t).find(_.name == name)
        block match {
          case Some(b) => Ok(views.html.webscala.displayBlockTeacher(b))
          case None => Redirect(routes.WebScala.myBlocks).flashing(("error") -> "This class does not exist")
        }
      }
      case Some(s: Student) => {
        val block = Block.getByStudent(s).find(_.name == name)
        block match {
          case Some(b) => Ok(views.html.webscala.displayBlockStudent(b))
          case None => Redirect(routes.WebScala.myBlocks).flashing(("error") -> "This class does not exist")
        }
      }
      case None => Redirect(routes.Application.index()).flashing(("error") -> "You must be logged in to see your classes.")
    }
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
  
  def joinBlock() = VisitAction {implicit req =>
    req.visit.user match {
      case Some(s: Student) => {
        if(req.method == "GET") {
          Ok(views.html.webscala.joinBlock(Binding(JoinBlockForm)))
        } else {
          Binding(JoinBlockForm, req) match {
            case ib: InvalidBinding => Ok(views.html.webscala.joinBlock(ib))
            case vb: ValidBinding => {
              Block.getByName(vb.valueOf(JoinBlockForm.blockName)) match {
                case Some(b: Block) => {
                  if(b.students.contains(s)) {
                    Redirect(routes.WebScala.joinBlock()).flashing(("error") -> "You are already a member of this class.")
                  } else {
                    b.addStudent(s)
                    DataStore.pm.makePersistent(b)
                    s.root.addDirectory(new Directory(b.name, s, Nil))
                    DataStore.pm.makePersistent(s)
                    Redirect(routes.WebScala.myBlocks).flashing(("success") -> ("You have been added to this class, and a class directory" 
                                                                               + " has been added to your home folder."))
                  }
                }
                case None => {
                  Redirect(routes.Application.index()).flashing(("error") -> "The class was not found by some strange course of events.")
                }
              }     
            }
          }
        }
      }
      case Some(_) => {
        Redirect(routes.Application.index()).flashing(("error") -> "You must be logged in as a student to join a class.")
      }
      case None => {
        Redirect(routes.Application.index()).flashing(("error") -> "You must be logged in to join a class.")
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
    req.visit.user match {
      case Some(t: Teacher) => {
        def maybeBlock = Block.getByTeacher(t).find(_.name == block)
        maybeBlock match {
          case Some(b) => {
            if(req.method == "GET") Ok(views.html.webscala.newAssignment(Binding(NewAssignmentForm(b))))
            else {
              Binding(NewAssignmentForm(b), req) match {
                case ib: InvalidBinding => Ok(views.html.webscala.newAssignment(ib))
                case vb: ValidBinding => {
                  val assignName = vb.valueOf(NewAssignmentForm(b).assignmentName).trim
                  val assignment = new Assignment(assignName, "", "")
                  b.addAssignemnt(assignment)
                  DataStore.pm.makePersistent(b)
                  Redirect(routes.WebScala.editAssignment(b.name, assignName))
                }
              }
            }
          }
          case None => Redirect(routes.Application.index()).flashing(("error") -> "Block not found.")
        }
      }
      case _ => Redirect(routes.Application.index()).flashing(("error") -> "You must be logged in as a teacher to create an assignment.")
    }
  }
  
  def editAssignment(block: String, assignment: String) = VisitAction {implicit req =>
    req.visit.user match {
      case Some(t: Teacher) => {
        def maybeBlock = Block.getByTeacher(t).find(_.name == block)
        maybeBlock match {
          case None => Redirect(routes.Application.index()).flashing(("error") -> "This class was not found")
          case Some(b) => {
              def maybeAssign = b.assignments.find(_.title == assignment)
              maybeAssign match {
                case None => Redirect(routes.Application.index()).flashing(("error") -> "This class was not found")
                case Some(a) => {
                  if(req.method == "GET") {
                    Ok(views.html.webscala.editAssignment(b, a))
                  } else {
                    val startCode = req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("start", Nil) match {
                      case Nil => ""
                      case fst :: rst => fst
                    }
                    val testCode = req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("test", Nil) match {
                      case Nil => ""
                      case fst :: rst => fst
                    }
                    a.starterCode_=(startCode)
                    a.testCode_=(testCode)
                    DataStore.pm.makePersistent(b)
                    Redirect(routes.WebScala.editAssignment(b.name, a.title)).flashing(("success") -> "Assignment updated")
                  }
                }
              }
            }
        }
      }
      case _ => Redirect(routes.Application.index()).flashing(("error") -> "You must be logged in as a teacher to edit an assignment.")
    }
  }
  
  def startAssignment(block: String, assignment: String) = VisitAction { implicit req =>
    req.visit.user match {
      case Some(u: User) => {
        def maybeBlock = Block.getByName(block)
        maybeBlock match {
          case Some(b) => {
            def maybeAssign = b.assignments.find(_.title == assignment)
            maybeAssign match {
              case Some(a) => {
                def starterCode = a.starterCode
                def maybeClassDir = u.root.content.find(_.title == block)
                maybeClassDir match {
                  case Some(d: Directory) => {
                    d.addFile(new File(a.title, u, starterCode))
                    DataStore.pm.makePersistent(u)
                    Redirect(routes.WebScala.fileManager(block + "/" + assignment))
                  }
                  case _ => Redirect(routes.Application.index()).flashing(("error") -> "A folder for this class was not found, so the assignment could not be added. Create a folder with this class's name in your home folder.")
                }
              }
              case None => Redirect(routes.Application.index()).flashing(("error") -> "This file was not found.")
            }
          }
          case None => Redirect(routes.Application.index()).flashing(("error") -> "This class was not found.")
        }
      }
      case _ => Redirect(routes.Application.index()).flashing(("error") -> "You are not logged in.")
    }
    
  }
}