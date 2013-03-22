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
}