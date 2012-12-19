package controllers

import scala.tools.nsc.interpreter.{ Results => IntpResults }

import play.api._
import play.api.mvc._
import webscala.HtmlRepl

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
}