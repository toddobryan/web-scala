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


object WebScala extends Controller {
  //lazy val repl = new HtmlRepl()

  def ide = Authenticated { implicit req =>
    Okay(views.html.webscala.ide())
  }
}