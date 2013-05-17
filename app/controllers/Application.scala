package controllers

import play.api._
import play.api.mvc._
import models.auth.VisitAction
import util.ControllerHelpers._

object Application extends Controller {
  
  def index = VisitAction { implicit req =>
    Okay(views.html.index("Your new application is ready."))
  }
  
}