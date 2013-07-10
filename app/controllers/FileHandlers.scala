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

object FileHandlers extends Controller {
  
   def fileIde(titles: String) = VisitAction { implicit req =>
    asUser { u => 
      val titleList = titles.split("/").toList
      val root = Directory.getUserRoot(u)
      val matchingFile = root.findItem(titleList)
      withFile(matchingFile) { f =>
        val result = SafeCode.runCode { (new HtmlRepl).repl.interpret(f.content) }
        Ok(views.html.webscala.ideSkeleton(f, result._2))
      }
    }
  }
  
   def interpret = VisitAction { implicit req =>
    println(req.body.asFormUrlEncoded.getOrElse(Map()))
    val line = getParameter(req, "line")
    println(line)
    val result = SafeCode.runCode { (new HtmlRepl).repl.interpret(line) }
    result._1 match {
      case IntpResults.Success => Ok( HtmlRepl.resultToHtml((new HtmlRepl).repl.valueOfTerm((new HtmlRepl).repl.mostRecentVar)) )
      case IntpResults.Error => Ok(result._2)
      case IntpResults.Incomplete => Ok(result._2)
    }
  }
  
  def compile(titles: String) = VisitAction { implicit req =>
    (new HtmlRepl).repl.reset()
    val content = getParameter(req, "line")
    req.visit.user match {
      case None => false
      case Some(user) => {
        val root = Directory.getUserRoot(user)
        root.findItem(titles.split("/").toList) match {
          case None => println("Error in saving file.")
          case Some(_: Directory) => println("This is a directory. What happened?")
          case Some(f: File) => {
            f.content_=(content)
            f.lastModified_=(DateTime.now)
            DataStore.pm.makePersistent(f)
            println("No errors in compiling")
          }
        }
      }
    }
    val result = SafeCode.runCode { (new HtmlRepl).repl.interpret(content) }
    Ok(result._2)
  }
  
  def save(titles: String) = VisitAction { implicit req =>
  	val content = getParameter(req, "content")
  	val testCode = getParameter(req, "test")
  	req.visit.user match {
      case None => false
      case Some(user) => {
        val root = Directory.getUserRoot(user)
        root.findItem(titles.split("/").toList) match {
          case None => println("Error in saving file test.")
          case Some(_: Directory) => println("This is a directory. What happened?")
          case Some(f: File) => {
            f.content_=(content)
            f.tests_=(testCode)
            f.lastModified_=(DateTime.now)
            DataStore.pm.makePersistent(f)
            println("No errors in saving")
          }
        }
      }
    }
  	Ok("Content and Tests saved.")
  }
  
  def test(titles: String) = VisitAction { implicit req =>
    req.visit.user match {
      case None => Redirect(routes.Application.index).flashing(("error" -> "You must be logged into test files"))
      case Some(user) => {
        val root = Directory.getUserRoot(user)
        root.findItem(titles.split("/").toList) match {
          case None => Redirect(routes.Application.index).flashing(("error" -> "File not found."))
          case Some(_: Directory) => Redirect(routes.Application.index).flashing(("error" -> "This is a directory"))
          case Some(f: File) => {
            val contentRes = SafeCode.runCode { (new HtmlRepl).repl.interpret(f.content) }
            contentRes._1 match {
              case IntpResults.Success | IntpResults.Incomplete => {
                val testRes = SafeCode.runCode { (new HtmlRepl).repl.interpret(f.tests) }
                testRes._1 match {
                  case IntpResults.Success | IntpResults.Incomplete => {
                    Ok(views.html.webscala.printTestResults(f.runOwnTest))
                  }
                  case _ => Ok(views.html.webscala.printTestResults("The following error occurred while running your test code: \n" + testRes._2))
                }
              }
              case _ => Ok(views.html.webscala.printTestResults("The following error occurred while running your file contents: \n" + contentRes._2))
            }
          }
        }
      }
    }  
  }
}