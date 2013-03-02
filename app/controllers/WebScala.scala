package controllers

import scala.tools.nsc.interpreter.{ Results => IntpResults }
import play.api._
import play.api.mvc._
import webscala.HtmlRepl
import scalajdo._
import models.files._
import models.auth.VisitAction
import models.auth.Authenticated

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
  
  def compile = VisitAction { implicit req =>
    println("Compiling content of files")
    val content = req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("line", Nil) match {
      case Nil => ""
      case fst :: rst => fst
    }
    val start = HtmlRepl.out.getBuffer.length
    HtmlRepl.repl.interpret(content) match {
      case IntpResults.Success => Ok("No errors in compiling this file!")
      case _ => Ok(HtmlRepl.out.getBuffer.substring(start))
    }
  }
  
  def showFile(id: Long) = VisitAction { implicit req =>
        val maybeFile = File.getById(id)
    	maybeFile match {
    	  case Some(f) => Ok(views.html.webscala.getFile(f))
    	  case None => Ok(views.html.index("apple"))
    	}
    }
}