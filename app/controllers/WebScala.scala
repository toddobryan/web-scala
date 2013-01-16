package controllers

import scala.tools.nsc.interpreter.{ Results => IntpResults }

import play.api._
import play.api.mvc._
import webscala.HtmlRepl
import scalajdo._
import models.files._

object WebScala extends Controller {
  //lazy val repl = new HtmlRepl()

  def ide = Action {
    Ok(views.html.webscala.ide())
  }

  def interpret = Action { implicit req =>
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
  
  def compile = Action { implicit req =>
    println("Compiling content of files")
    val content = req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse("line", Nil) match {
      case Nil => ""
      case fst :: rst => fst
    }
    val start = HtmlRepl.out.getBuffer.length
    HtmlRepl.repl.compileString(content) match {
      case true => Ok("No errors in compiling this file!")
      case false => Ok(HtmlRepl.out.getBuffer.substring(start))
    }
  }
  
  def showFile(id: Long) = Action { implicit req =>
    	implicit val pm: ScalaPersistenceManager = DataStore.pm
    	val maybeFile = File.getById(id)
    	maybeFile match {
    	  // case Some(f) => views.html.webscala.showFile(f)
    	  // case None => views.html.webscala.notFound()
    	  case _ => Ok(views.html.index("Dog"))
    	}
    }
}