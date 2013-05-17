package webscala

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.Results._
import java.io.StringWriter
import java.io.PrintWriter
import scala.annotation.Annotation
import scala.util.{Try, Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.tools.nsc.interpreter.{ Results => IntpResults }
import scala.tools.nsc.interpreter.IR.{ Result => IntpResult}

import org.dupontmanual.image.{ Bitmap, Image }

class HtmlRepl {
  
}

object SafeCode {
  
  def runCode(code: => IntpResult): (IntpResult, String) = {
    val start = HtmlRepl.out.getBuffer.length
    val res = future { code }
    try {
      val eventualResult = Await.result(res, 10000 millis)
      eventualResult match {
        case IntpResults.Success => (IntpResults.Success, "No errors!")
        case IntpResults.Incomplete => (IntpResults.Incomplete, "Your code was not complete. Check near the end.")
        case IntpResults.Error => (IntpResults.Error, "The following error occurred:\n" + HtmlRepl.out.getBuffer.substring(start))
      }
    } catch {
      case to: java.util.concurrent.TimeoutException => (IntpResults.Error, "Timeout Exception. Check for infinite loops.")
      case e: Exception => (IntpResults.Error, "Exception Thrown: " + e)
    }
  }
  
}

object HtmlRepl {
  val webscalaTester = {
"""def webscalaTester(tests: List[(String, Boolean, String)]): List[(String, Boolean)] = {
    val testResults = for(test <- tests if !test._2) yield 
      (test._1 + " failed. " + test._3, false)
    val passedTests = tests.length - testResults.length
    val success = {
      if(passedTests == 0) Nil
      else List((passedTests + " tests passed!", true))
    }
    testResults ++ success
  }
    
   webscalaTester(myTests)"""
  }
  
  lazy val out = new StringWriter()
  
  val repl = {
    val settings = new Settings
    settings.embeddedDefaults(new ReplClassLoader(settings.getClass().getClassLoader()))
    val theRepl = new IMain(settings, new PrintWriter(out)) {
      override protected def parentClassLoader: ClassLoader = this.getClass.getClassLoader()
    }
    theRepl.addImports("scala.language.ImplicitConversions")
    theRepl.addImports("org.dupontmanual.image._")
    theRepl
  }
  
  def resultToHtml(res: Option[AnyRef]): String = res match {
    case None => ""
    case Some(x) => x match {
      case img: Image => <img class="image-obj" src={ "data:image/png;base64,%s".format(img.asInstanceOf[Image].base64png) } />.toString
      case _ => x.toString
    }
  }
}

