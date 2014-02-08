package controllers

import play.api._
import play.api.mvc._
import models.auth.VisitAction
import util.ControllerHelpers._

object Application extends Controller {
  
  def index = VisitAction { implicit req =>
    Ok(views.html.index(req.remoteAddress))
  }
  
}