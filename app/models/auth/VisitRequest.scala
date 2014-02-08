package models.auth

import play.api.mvc._
import javax.jdo.JDOHelper
import play.mvc.Results.Redirect
import scalajdo.DataStore
import util.UsesDataStore

case class VisitRequest[A](visit: Visit, private val request: Request[A]) extends WrappedRequest[A](request)

// TODO: we need a cache system
object VisitAction extends UsesDataStore {
  def apply(block: VisitRequest[AnyContent] => SimpleResult): Action[AnyContent] = {
    apply[AnyContent](BodyParsers.parse.anyContent)(block)
  }

  def apply[A](p: BodyParser[A])(f: VisitRequest[A] => SimpleResult): Action[A] = {
    Action(p)(request => {
      dataStore.withTransaction { pm =>
        val visitReq = VisitRequest[A](Visit.getFromRequest(request), request)
        val res = f(visitReq)
        if (JDOHelper.isDeleted(visitReq.visit)) {
          res.withNewSession
        } else {
          visitReq.visit.expiration = System.currentTimeMillis + Visit.visitLength
          if (request.session.get(visitReq.visit.uuid).isDefined) res
          else res.withSession("visit" -> visitReq.visit.uuid)
        }
      }
    })
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