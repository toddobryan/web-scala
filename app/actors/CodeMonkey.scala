package actors

import scala.tools.nsc.interpreter.{ Results => IntpResults }
import controllers.routes
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
import util.QuickRedirects._
import akka.actor._
import actors._
import scala.annotation.Annotation
import scala.util.{Try, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.language.postfixOps

object TimeoutFuture

class CodeMonkey extends Actor {
  val htmlRepl = new HtmlRepl()
  
  def receive = {
    case c: String => checkCode(c, sender)
    case _: Any => throw new IllegalArgumentException
  }
  
  def checkCode(code: String, director: ActorRef) = {
    import IntpResults._
    import htmlRepl._
    
    val start = out.getBuffer.length
    def withError(res: IntpResult) = (res, out.getBuffer.substring(start))
    
    val almostResult = future { repl.interpret(code) }
    try {
      val res = Await.result(almostResult, 10000 millis)
      director ! res
    } catch {
      case e: Exception => Error
    }
  }
}