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
import forms._
import forms.fields._
import forms.validators._

object QuickRedirects extends Controller {
  
  // Type Aliases
  type VRequest = VisitRequest[AnyContent]
  type ToResult[T] = T => PlainResult
  
  // Redirects to the login page
  object LoginError {
   def apply(message: String)(implicit req: VRequest): PlainResult = {
      Redirect(routes.Application.index()).flashing(("error" -> message))
    }
  }
  
  // An object to create generic redirects with changing messages.
  object Error {
    def apply(message: String)(implicit req: VRequest): PlainResult = {
      Redirect(req.visit.redirectUrl.getOrElse(routes.Application.index.url)
        ).flashing(("error" -> message)) 
    }
    
    def apply(implicit req: VRequest): PlainResult = {
      apply("An error occurred while trying to access this page.")
    }
  }
  
  object UserRedirect {
    def apply()(implicit req: VRequest): PlainResult = {
      LoginError("You must be logged in as a user for this request.")
    }
  }
}