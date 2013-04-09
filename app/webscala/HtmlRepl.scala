package webscala

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.interpreter.Results._
import java.io.StringWriter
import java.io.PrintWriter
import scala.annotation.Annotation
import scala.util.{Try, Success, Failure}

class HtmlRepl {
  
}

object HtmlRepl {
  val webscalaTester = {
"""def webscalaTester(tests: List[(String, Boolean, String)]): List[(String, Boolean)] = {
    val testResults = for(test <- tests if !test._2) yield 
      (test._1 + " failed. " + test._3, false)
    val passedTests = tests.length - testResults.length
    val success = {
      if(passedTests == 0) Nil
      else List((passedTests + " tests passed!", true))
    }
    testResults ++ success
  }
    
   webscalaTester(myTests)"""
  }
  
  lazy val out = new StringWriter()
  
  val repl = {
    val settings = new Settings
    settings.embeddedDefaults(new ReplClassLoader(settings.getClass().getClassLoader()))
    val theRepl = new IMain(settings, new PrintWriter(out))
    theRepl.addImports("scala.language.ImplicitConversions")
    theRepl.addImports("image._")
    theRepl
  }
  
  // Jim's stuff for testing.
  
  def isCorrectType(toCheck: Option[AnyRef]): Boolean = {
    toCheck match {
      case Some(l: List[_]) => {
        l match {
          case Nil => true
          case x :: xs => {
            x match {
              case p: (_, _) => {
                p._1 match {
                  case s: String => {
                    p._2 match {
                      case b: Boolean => true
                      case _ => false
                    }
                  }
                  case _ => false
                }
              }
              case _ => false
            }
          }
        } 
      }
      case _ => false
    }
  }
  
  def unwrapTests(theTests: Option[AnyRef]): List[(String, Boolean)] = {
    theTests match {
      case Some(tests: List[_]) => {
       for(test <- tests) yield {
         test match {
           case p: (_, _) => {
             p._1 match {
               case s: String => {
                 p._2 match {
                   case b: Boolean => (s, b)
                 }
               }
             }
           }
         }
       }
      }
    }
  }
}

