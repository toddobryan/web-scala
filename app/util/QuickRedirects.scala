package util

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
import org.dupontmanual.forms
import forms._
import forms.fields._
import forms.validators._

object QuickRedirects extends Controller {
  
  // Type Aliases
  type VRequest = VisitRequest[AnyContent]
  type ToResult[T] = T => SimpleResult
  
  // Redirects to the login page
  object LoginError {
   def apply(message: String)(implicit req: VRequest): SimpleResult = {
      Redirect(routes.Application.index()).flashing(("error" -> message))
    }
  }
  
  // An object to create generic redirects with changing messages.
  object Error {
    def apply(message: String)(implicit req: VRequest): SimpleResult = {
      Redirect(req.visit.redirectUrl.getOrElse(routes.Application.index.url)
        ).flashing(("error" -> message)) 
    }
    
    def apply(implicit req: VRequest): SimpleResult = {
      apply("An error occurred while trying to access this page.")
    }
  }
  
  object UserRedirect {
    def apply()(implicit req: VRequest): SimpleResult = {
      LoginError("You must be logged in as a user for this request.")
    }
  }
}