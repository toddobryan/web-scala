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
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.util.Timeout
import actors._
import ExecutionContext.Implicits.global


class CodeDirector(val id: String) extends Actor {
  import CodeDirector._
  
  var addedCode = 0
  
  def receive = {
    case str: String => {
      val res = run(str) 
      println(res)
    }
    case _ => println("Message received.")
  }
  
  override val supervisorStrategy = 
    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 minute) {
      case _ => Stop
  	}
  
  implicit val timeout = Timeout(15 seconds)
  
  def run(code: String): (IntpResult, String) = {
    import IntpResults._
    val name = id + "monkey" + addedCode
    val monkey = context.child(name) match {
      case Some(m) => m
      case None => context.actorOf(Props[CodeMonkey], name)
    }
    val askMonkey = (monkey ? CodeToRun(code))
    try {
      Await.result(askMonkey, 10 seconds) match {
        case Error => (Error, "Exception thrown")
        case x: IntpResult => (x, "No exceptions thrown")
        case _ => (Error, "CodeMonkey did not send correct value")
      }
    } catch {
      case _: Throwable => {
        monkey ! Stop
        addedCode += 1
        (Error, "Did not receive in 10 seconds")
      }
    }
  }
}

object CodeDirector {
  def props(id: String) = Props(classOf[CodeDirector], id)
  
  case class CodeToRun(code: String)
}
