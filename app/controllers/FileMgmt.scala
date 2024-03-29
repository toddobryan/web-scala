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
import org.dupontmanual.forms
import forms._
import forms.fields._
import forms.validators._
import util.ControllerHelpers._
import models.files.Directory

object FileMgmt extends Controller {
  
  case class NewFileForm(val dir: Directory) extends Form {
    val fileName = new TextField("fileName")
    val dirOrFile = new ChoiceField("Type", List(("File", "file"), ("Directory", "dir")))
    def fields = List(fileName, dirOrFile)
    
    override def validate(vb: ValidBinding): ValidationError = {
      // TODO Fix
      Directory.getItems(dir).filter(_.title == vb.valueOf(NewFileForm(dir).fileName).trim) match {
        case Nil => ValidationError(Nil)
        case file :: _ => ValidationError("File with this name already exists.")
      }
    }
  }
  
  def newFileHome = newFile("")
  
  def newFileHomeP = newFile("")
  
  // TODO: Fix with new File Stuff
  def newFileActions(user: User, titles: String, dir: Directory)(implicit req: VRequest):(SimpleResult, SimpleResult) = { 
    formHandle(form = NewFileForm(dir), title = "Add File") {
      vb: ValidBinding => {
        val name = vb.valueOf(NewFileForm(dir).fileName).trim()
        val dirOrFile = vb.valueOf(NewFileForm(dir).dirOrFile)
        if(dirOrFile == "dir") {
          val newDir = new Directory(name, user, 0)
        } else {
          //TODO : Fix val file = new File(name, user, "/* Insert Code Here */", Some(DateTime.now))
        }
        if(titles == "") {
          Redirect("/fileManager").flashing(("success" -> "Item Created"))
        } else {
          Redirect("/fileManager/" + titles).flashing(("success" -> "Item Created"))
        }
      }
    }
  }
  
  def newFile(titles: String) = VisitAction { implicit req =>
    val titlesList = if(titles == "") Nil else titles.split("/").toList
    asUser { user => 
      val root = Directory.getUserRoot(user)
      implicit val maybeItem = Directory.getItem(titlesList)
      withDir { dir =>
        newFileActions(user, titles, dir)._1
      }
    }
  }
  
  def newFileP(titles: String) = VisitAction { implicit req =>
    val titlesList = if(titles == "") Nil else titles.split("/").toList
    asUser { user =>
      val root = Directory.getUserRoot(user)
      implicit val maybeItem = Directory.getItem(titlesList)
      withDir { dir =>
        newFileActions(user, titles, dir)._2
      }
    }
  }
  
  def fileManagerHome() = VisitAction { implicit req =>
    asUser { user => 
      val root = Directory.getUserRoot(user)
      Okay(views.html.webscala.displayFiles(root, "")) 
    }
  }
  
 def fileManager(titles: String) = VisitAction { implicit req =>
    asUser { user =>
      val directory = Directory.getUserRoot(user)
      val titlesList = titles.split("/").toList
      findFile(directory, titlesList)
    }
 }

  def findFile(dir: Directory, title: List[String])(implicit req: VisitRequest[AnyContent]) = {
    val path = title.mkString("/")
    // TODO: Fix
    val maybeItem = Directory.getItem(title)
    dirOrFile(maybeItem) 
      { d: Directory => Okay(views.html.webscala.displayFiles(d, path)) }
      { f: File => Okay(views.html.webscala.getFile(f, path)) }
  }
  
    def submitFile(block: String, assignment: String) = VisitAction { implicit req => 
    asUser { u => 
      withBlock(Block.getByName(block)) { b => 
        implicit val maybeAssign = Assignment.getBlockAssignments(b).find(_.title == assignment)
        withAssignment { a =>
          // TODO: Fix
          implicit val matchingFile = Directory.getItem(/*.getUserRoot(u).*/List(block, assignment))
          withFile { f => Okay(views.html.webscala.showTestResults(a, f)) }
        }
      }
    }
  }
}