package controllers

import play.api._
import play.api.mvc._

object WebScala extends Controller {
  def ide = Action {
    Ok(views.html.webscala.ide())
  }
}