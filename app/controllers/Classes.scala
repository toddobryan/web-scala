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

object Classes extends Controller {
  
  object NewBlockForm extends Form {
    def blockName = new TextField("blockName")
    
    def fields = List(blockName)
    
    override def validate(vb: ValidBinding): ValidationError = {
      def blockName = vb.valueOf(NewBlockForm.blockName).trim
      Block.getByName(blockName) match {
        case None => ValidationError(Nil)
        case Some(_) => ValidationError(List("A block with this name already exists."))
      }
    }
  }
  
  def newBlockActions(t: Teacher)(implicit req: VRequest) = 
    formHandle(form = NewBlockForm, title= "Add Block") {
    vb: ValidBinding => 
      val blockName = vb.valueOf(NewBlockForm.blockName).trim
      val newBlock = new Block(blockName, t)
      DataStore.pm.makePersistent(newBlock)
      Redirect(routes.Classes.myBlocks()).flashing(("success" -> "New Class Created"))
  }
  
  def newBlock() = VisitAction { implicit req =>
    asTeacher { t => newBlockActions(t)._1 }
  }
  
  def newBlockP() = VisitAction { implicit req =>
    asTeacher { t => newBlockActions(t)._2 }
  }
  
  def myBlocks() = VisitAction { implicit req =>
    teacherOrStudent
      {t => Okay(views.html.webscala.displayBlocksTeacher(Block.getByTeacher(t)))}
      {s => Okay(views.html.webscala.displayBlocksStudent(Block.getByStudent(s)))}
  }
  
  def findMyBlock(name: String) = VisitAction {implicit req =>
    val tAction: ToResult[Teacher] = { t => 
      implicit val maybeBlock = Block.getByTeacher(t).find(_.name == name)
      withBlock { b: Block => Okay(views.html.webscala.displayBlockTeacher(b)) }
    }
    val sAction: ToResult[Student] = { s => 
      implicit val maybeBlock = Block.getByStudent(s).find(_.name == name)
      withBlock { b: Block => Okay(views.html.webscala.displayBlockStudent(b)) }
    }
    teacherOrStudent { tAction } { sAction}
  }
  
  object JoinBlockForm extends Form {
    def blockName = new TextField("blockName")
    def joinCode =  new TextField("joinCode")
    
    def fields = List(blockName, joinCode)
    
    override def validate(vb: ValidBinding): ValidationError = {
      Block.getByName(vb.valueOf(JoinBlockForm.blockName)) match {
        case Some(b: Block) => {
          if(b.id.toString == vb.valueOf(joinCode)) {
            new ValidationError(Nil)
          } else {
            new ValidationError(List("Class and Join Code did not match."))
          }
        }
        case None => {
          new ValidationError(List("Class could not be found."))
        }
      }
    }
  }
  
  def joinBlockActions(s: Student)(implicit req: VRequest) = 
    formHandle( form = JoinBlockForm, title = "Join Block") {
      vb: ValidBinding =>
        implicit val maybeBlock = Block.getByName(vb.valueOf(JoinBlockForm.blockName))
        withBlock { b =>
        if(b.students.contains(s))  Redirect(routes.Classes.joinBlock()).flashing(("error") -> "You are already a member of this class.")
        else {
          b.addStudent(s)
          DataStore.pm.makePersistent(b)
          val root = Directory.getUserRoot(s)
          root.addDirectory(new Directory(b.name, s, Nil))
          DataStore.pm.makePersistent(s)
          Redirect(routes.Classes.myBlocks).flashing(("success") -> ("You have been added to this class, and a class directory" 
                                                                       + " has been added to your home folder."))
      }
    }
  }
  
  def joinBlock() = VisitAction { implicit req =>
    asStudent { s => joinBlockActions(s)._1 }
  }
  
  def joinBlockP() = VisitAction { implicit req =>
    asStudent { s => joinBlockActions(s)._2}
  }
}