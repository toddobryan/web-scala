package controllers

import scala.tools.nsc.interpreter.{ Results => IntpResults }
import play.api._
import play.api.mvc._
import webscala.HtmlRepl
import scalajdo._
import models.files._
import models.auth.VisitAction
import models.auth.Authenticated
import forms._
import forms.fields._

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
  
  def compile(id: Long) = VisitAction { implicit req =>
    println("Compiling content of files")
    val content = req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("line", Nil) match {
      case Nil => ""
      case fst :: rst => fst
    }
    File.getById(id) match {
      case None => false
      case Some(f) => {
        f.content_=(content)
        DataStore.pm.makePersistent(f)
      }
    }
    val start = HtmlRepl.out.getBuffer.length
    HtmlRepl.repl.interpret(content) match {
      case IntpResults.Success => Ok("No errors in compiling this file!")
      case _ => Ok(HtmlRepl.out.getBuffer.substring(start))
    }
  }
  
  object newFileForm extends Form {
    val fileName = new TextField("fileName")
    def fields = List(fileName)
  }
  
  def newFile = VisitAction { implicit req =>
    req.visit.user match {
        case None => Redirect(routes.Application.index()).flashing(("error" -> "You must be logged in to create a file"))
        case Some(user) => {
          if(req.method == "GET") {
            Ok(views.html.webscala.newFile(Binding(newFileForm)))
          } else {	
            Binding(newFileForm, req) match {
              case ib: InvalidBinding => Ok(views.html.webscala.newFile(ib))
              case vb: ValidBinding => {
                val name = vb.valueOf(newFileForm.fileName)
                val file = new File(name, user, "/* Enter Code Here */")
                DataStore.pm.makePersistent(file)
                val id = File.getByOwner(user).filter(_.title == name).head.id
                Redirect("/file/" + id).flashing(("success" -> "File Created"))
              }
            }
         }
       }
    }
  }
  
  def showFile(id: Long) = VisitAction { implicit req =>
        val maybeFile = File.getById(id)
    	maybeFile match {
    	  case Some(f) => Ok(views.html.webscala.getFile(f))
    	  case None => Ok(views.html.index("File not found"))
    	}
    }
}