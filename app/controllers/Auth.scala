package controllers

import play.api.mvc.Controller
import forms.{Form, ValidBinding}
import forms.fields.{TextField, PasswordField}
import models.auth.VisitAction
import forms.validators.ValidationError
import scalajdo.DataStore
import models.auth.Method
import models.auth.{User, Student}
import forms.{Binding, InvalidBinding, ValidBinding}


object LoginForm extends Form {
  val username = new TextField("username")
  val password = new PasswordField("password")
  
  def fields = List(username, password)
  
  override def validate(vb: ValidBinding): ValidationError = {
    User.authenticate(vb.valueOf(username), vb.valueOf(password)) match {
      case None => ValidationError("Incorrect username or password.")
      case Some(user) => ValidationError(Nil)
    }
  }
}

object NewUserForm extends Form {
  val username = new TextField("username")
  val firstName = new TextField("firstName")
  val lastName = new TextField("lastName")
  val password = new PasswordField("password")
  val confirmPassword = new PasswordField("confirmPassword")
  
  def fields = List(username, firstName, lastName, password, confirmPassword)
  
  override def validate(vb: ValidBinding): ValidationError = {
    User.getByUsername(vb.valueOf(username)) match {
      case Some(user) => ValidationError("User with given name already exists.")
      case None => {
        val pass1 = vb.valueOf(password)
        val pass2 = vb.valueOf(confirmPassword)
        if(pass1.equals(pass2) && pass1.length() > 5) {
          ValidationError(Nil)
        } else if(pass1.equals(pass2)) {
          ValidationError("Password must be 6 or more characters.")
        } else {
          ValidationError("Passwords did not match")
        }
      }
    }
  }
}

object Auth extends Controller {
  def login() = VisitAction { implicit req => 
    if (req.method == Method.GET) {
      Ok(views.html.auth.login(Binding(LoginForm)))
    } else {
      Binding(LoginForm, req) match {
        case ib: InvalidBinding => Ok(views.html.auth.login(ib))
        case vb: ValidBinding => {
          // set the session user
          req.visit.user = User.getByUsername(vb.valueOf(LoginForm.username))
          val redirectUrl: Option[String] = req.visit.redirectUrl
          req.visit.redirectUrl = None
          DataStore.pm.makePersistent(req.visit)
          redirectUrl.map(Redirect(_)).getOrElse(Redirect(routes.Application.index())).flashing("success" -> "You have successfully logged in.")
        }
      }
    }
  }
  
  def logout() = VisitAction { implicit req =>
    DataStore.pm.deletePersistent(req.visit)
    Redirect(routes.Application.index()).flashing("success" -> "You have successfully logged out.") 
  }
  
  def newUser = VisitAction { implicit req =>
  	req.visit.user match {
  	  case Some(_) => Redirect(routes.Application.index()).flashing("error" -> "You are already logged in!")
  	  case None => {
  	    if(req.method == Method.GET) {
  	      Ok(views.html.auth.newUserForm(Binding(NewUserForm)))
  	    } else {
  	      Binding(NewUserForm, req) match {
  	        case ib: InvalidBinding => Ok(views.html.auth.newUserForm(ib))
  	        case vb: ValidBinding => {
  	          val info = Map("username" -> vb.valueOf(NewUserForm.username), "password" -> vb.valueOf(NewUserForm.password), 
  	        		  		 "first" -> vb.valueOf(NewUserForm.firstName), "last" -> vb.valueOf(NewUserForm.lastName))
  	          val user: User = new Student(info("username"), first = info("first"), last = info("last"), password = info("password"))
  	          DataStore.pm.makePersistent(user)
  	          Redirect(routes.Auth.login()).flashing("success" -> "Account created successfully! Now log in.")
  	        }
  	      }
  	    }
  	  }
  	}
  }
}
