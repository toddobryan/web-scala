package models.files

import javax.jdo.annotations._
import org.datanucleus.api.jdo.query._
import org.datanucleus.query.typesafe._
import org.joda.time.DateTime
import scalajdo._
import models.auth._
import scala.tools.nsc.interpreter.{ Results => IntpResults }
import webscala._
import util.{ UsesDataStore, TestableItem, TestableFile }

sealed trait DisplayableItem {
  def asHtmlFO: scala.xml.Elem
  def asHtmlFO(path: String): scala.xml.Elem
  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem
}

trait DisplayableFile extends DisplayableItem {
  def title: String
  def timeString: String
  def id: Long
  
  def teacherTestUrl(blockString: String, studentString: String, pathToDir: String): String ={
    "/myClasses/" + blockString + "/" + studentString + "/" + "testCode" + "/" + {if(pathToDir == "") "" else "/"} + title
  }
  
  def asHtmlFO: scala.xml.Elem = {
    <li class="file-fo">
	  <a href={"/file/" + id}>
		<div class="file-title span6">{title}</div>
		<div class="file-modified span2">{timeString}</div>
      </a>
      <form class="in-line">
		<button method="post" action={"/deleteFile/" + id}>
			Delete?
		</button>
	  </form>
    </li>
  }
  
  def asHtmlFO(pathToDir: String): scala.xml.Elem = {
    <li class="file-fo">
	  <a href={"/fileManager/" + pathToDir + {if(pathToDir == "") "" else "/"} + title}>
		<div class="file-title span6">{title}</div>
		<div class="file-modified span2">{timeString}</div>
      </a>
    </li>
  }
  
  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem = {
     <li class={"file-fo"}>
       <a href={"/myClasses/" + blockString + "/" + studentString + "/" + {if(pathToDir == "") "" else "/"} + title}>
         <div class="file-title span4">{title}</div>
		 <div class="file-modified span2">{timeString}</div>
       </a>
	   <div class="span3"><button href={teacherTestUrl(blockString, studentString, pathToDir)} class="btn test-btn">Test Code</button></div>
     </li>
   }
}

trait DisplayableDirectory extends DisplayableItem {
  def title: String
  
    // F.O. => File Organizer
  def asHtmlFO: scala.xml.Elem = {
    <li class={ "folder-fo" }>
      <div class="folder-name">{ title }</div>
    </li>
  }

  def asHtmlFO(pathToDir: String): scala.xml.Elem = {
    <li class={ "folder-fo" }>
      <a href={ "/fileManager/" + pathToDir + { if (pathToDir == "") "" else "/" } + title }>
        <div class="folder-name">{ title }</div>
      </a>
    </li>
  }

  def asBlockFO(blockString: String, studentString: String, pathToDir: String): scala.xml.Elem = {
    <li class={ "folder-fo" }>
      <a href={ "/myClasses/" + blockString + "/" + studentString + "/" + { if (pathToDir == "") "" else "/" } + title }>
        <div class="folder-name">{ title }</div>
      </a>
    </li>
  }
}