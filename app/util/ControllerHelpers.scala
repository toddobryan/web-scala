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
import util.QuickRedirects._
import models.files.Directory


object ControllerHelpers extends Controller {
  
  // Type Aliases
  type VRequest = VisitRequest[AnyContent]
  type ToResult[T] = T => SimpleResult

  /* To help with redirection, this is WebScala's own Ok method, which Oks the result
   * while also updating the request redirectTo url redirect to.
   */
  object Okay {
    def apply[C](content: C)(implicit writable: http.Writeable[C], req: VRequest) = {
      req.visit.redirectUrl = Some(req.uri)
      Ok(content)
    }
  }
  
  /* These methods are to eliminate the endless indenting that
   * comes with pattern matching of the visit user to
   * a student or a teacher, and also to reduce redundancy of 
   * redirection.
   */
  
  // Requires there to be an active user; redirects to error if there isn't
  def asUser(ufun: ToResult[User])(implicit req: VRequest): SimpleResult = {
    req.visit.user match {
      case Some(u) => ufun(u)
      case None => UserRedirect()
    }
  }
  
  
  // Matches to a student or teacher and acts accordingly
  def teacherOrStudent(tfun: ToResult[Teacher])(sfun: ToResult[Student])
  					  (implicit req: VRequest): SimpleResult = {
    val errMes = "You are neither a student or teacher. Contact a system administrator."
    asUser { _ match { case t: Teacher => tfun(t) 
    				   case s: Student => sfun(s)
    				   case _ => Error(errMes) } }
  }
  
  // Redirects if a teacher is not logged in
  def asTeacher(tfun: ToResult[Teacher])(implicit req: VRequest): SimpleResult = {
    val sfun: ToResult[Student] = s => Error("You must be logged in as a teacher for this request.")
    teacherOrStudent(tfun)(sfun)
  }
  
  // Redirects if a student is not logged in
  def asStudent(sfun: ToResult[Student])(implicit req: VRequest): SimpleResult = {
    val tfun: ToResult[Teacher] = t => Error("You must be logged in as a student for this request.")
    teacherOrStudent(tfun)(sfun)
  }
  
  /* The following method is meant to standardize the form process
   * and reduce the copy pasta that all the form controllers have.
   * Given a post request and a valid binding, it will call the 
   * BindingToResult parameter on the valid binding.
   */
  def formHandle(form: Form, title: String = "Create/Modify")(ba: ToResult[ValidBinding])
  				(implicit req: VRequest): (SimpleResult, SimpleResult) = {
    (Ok(views.html.genericForm(Binding(form), title)),
     Binding(form, req) match {
       case ib: InvalidBinding => Ok(views.html.genericForm(ib))
       case vb: ValidBinding => ba(vb)
     })
  }
  
  /* These methods are like the User matching methods above,
   * except they are meant to match Items retrieved from a
   * User's root directory. They are overloaded with explicit
   * and implicit Option[T] parameters.
   */
  // Template for matching items and blocks from the database.
  def withObject[T](maybeObj: Option[T], errMes: String)
  				   (objfun: ToResult[T])(implicit req: VRequest): SimpleResult = {
    maybeObj match { case Some(o) => objfun(o); case None => Error(errMes) }
  }
  
  // withObject
  def withObject[T](errMes: String)
  				   (objfun: ToResult[T])(implicit req: VRequest, maybeObj: Option[T]): SimpleResult = {
    withObject(maybeObj, errMes)(objfun)
  }
  
  // An implementation of withObject for matching items.
  def withItem(maybeItem: Option[Item])(ifun: ToResult[Item])
  			  (implicit req: VRequest): SimpleResult = {
    val errMes = "No such item exists in your directory."
    withObject(maybeItem, errMes)(ifun)
  }
  
  def withItem(ifun: ToResult[Item])(implicit req: VRequest, maybeItem: Option[Item]): SimpleResult = {
    withItem(maybeItem)(ifun)
  }
  
  // Pattern matches an Option[Item] and acts depending on the result.
  def dirOrFile(maybeItem: Option[Item])(dfun: ToResult[Directory])(ffun: ToResult[File])
  			   (implicit req: VRequest): SimpleResult = {
    withItem(maybeItem) {
      _ match {case d: Directory => dfun(d)
               case f: File => ffun(f)
               case _ => Error("This item was neither a directory or file.")}
    }
  }
  
  // Pattern matches an Option[Item], and redirects if the item is not a directory.
  def withDir(maybeItem: Option[Item])(dfun: ToResult[Directory])(implicit req: VRequest): SimpleResult = {
    val ffun: ToResult[File] = f => Error("A directory is required for this action. A file was found.")
    dirOrFile(maybeItem)(dfun)(ffun)
  }
  
  def withDir(dfun: ToResult[Directory])(implicit req: VRequest, maybeItem: Option[Item]): SimpleResult = {
    withDir(maybeItem)(dfun)
  }
  
  // Pattern matches an Option[Item], and redirects if the item is not a file.
  def withFile(maybeItem: Option[Item])(ffun: ToResult[File])(implicit req: VRequest): SimpleResult = {
    val dfun: ToResult[Directory] = d => Error("A file is required for this action. A directory was found.")
    dirOrFile(maybeItem)(dfun)(ffun)
  }
  
  def withFile(ffun: ToResult[File])(implicit req: VRequest, maybeItem: Option[Item]): SimpleResult = {
    withFile(maybeItem)(ffun)
  }
  
  // An implementation of withObject for matching Blocks obtained from the database
  def withBlock(maybeBlock: Option[Block])(bfun: ToResult[Block])(implicit req: VRequest): SimpleResult = {
    val errMes = "No such item exists in your directory."
    withObject(maybeBlock, errMes)(bfun)
  }
  
  def withBlock(bfun: ToResult[Block])(implicit req: VRequest, maybeBlock: Option[Block]): SimpleResult = {
    withBlock(maybeBlock)(bfun)
  }
  
  // An implementation of withObject for matching Assignments obtained from the database
  def withAssignment(maybeAssign: Option[Assignment])(afun: ToResult[Assignment])
  					(implicit req: VRequest): SimpleResult = {
    val errMes = "No such assignment exists in this directory"
    withObject(maybeAssign, errMes)(afun)
  }
  
  def withAssignment(afun: ToResult[Assignment])
      (implicit req: VRequest, maybeAssignment: Option[Assignment]): SimpleResult = {
    withAssignment(maybeAssignment)(afun)
  }
  
  /* This class is for receiving parameters from an encoded request
   * without the need for excessive pattern matching.
   */ 
  def getParameter(req: VRequest, name: String): String = {
    req.body.asFormUrlEncoded.getOrElse(Map()).getOrElse(name, Nil) match {
      case Nil => ""
      case fst :: rst => fst
    }
  }
}
