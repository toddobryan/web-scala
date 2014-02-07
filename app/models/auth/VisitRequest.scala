package models.auth

import play.api.mvc._
import javax.jdo.JDOHelper
import play.mvc.Results.Redirect
import scalajdo.DataStore
import util.UsesDataStore

class VisitRequest[A](val request: Request[A]) extends WrappedRequest[A](request) {
  implicit val visit: Visit = request.session.get("visit").flatMap(
    Visit.getByUuid(_)).filter(!_.isExpired).getOrElse(
      new Visit(System.currentTimeMillis + Visit.visitLength, None))
}

// TODO: we need a cache system
object VisitAction extends UsesDataStore {
  def apply[A](p: BodyParser[A])(f: VisitRequest[A] => SimpleResult) = {
    Action(p)(request => {
      dataStore.withTransaction { pm =>
        val visitReq = new VisitRequest[A](request)
        val res = f(visitReq)
        if (JDOHelper.isDeleted(visitReq.visit)) {
          pm.commitTransaction()
          res.withNewSession
        } else {
          visitReq.visit.expiration = System.currentTimeMillis + Visit.visitLength
          pm.makePersistent(visitReq.visit)
          pm.commitTransaction()
          if (request.session.get(visitReq.visit.uuid).isDefined) res
          else res.withSession("visit" -> visitReq.visit.uuid)
        }
      }
    })
  }

  def apply(f: VisitRequest[AnyContent] => SimpleResult) = {
    apply[AnyContent](BodyParsers.parse.anyContent)(f)
  }
}

object Authenticated extends UsesDataStore {
  def apply(f: VisitRequest[AnyContent] => SimpleResult) = VisitAction(implicit req => {
    req.visit.user match {
      case None => {
        req.visit.redirectUrl = req.path
        dataStore.pm.makePersistent(req.visit)
        Results.Redirect(controllers.routes.Auth.login()).flashing("error" -> "You must log in to view that page.")
      }
      case Some(user) => {
        implicit val u: User = user
        f(req)
      }
    }
  })
}

object Method {
  val GET = "GET"
  val POST = "POST"
}