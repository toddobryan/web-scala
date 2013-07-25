package webscala

import java.lang.{Process, ProcessBuilder, Thread, StringBuffer}
import java.io.File
import java.net.URLClassLoader
import actors.CodeMonkey
import com.typesafe.config.ConfigFactory

class RunProcess {
  
  val configs = ConfigFactory.load("newjvm")
  val separator = System.getProperty("file.separator")
  val classLoaderType = {
    val classLoader = Thread.currentThread.getContextClassLoader()
    classLoader.getClass().toString()
  }
  /*val classpath = {
    val buffer = new StringBuffer
    val urls = 
      Thread.currentThread.getContextClassLoader match {
        case urlcl: URLClassLoader => urlcl.getURLs().toList
        case _ => throw new Exception("Not a URL Class Loader")
      }
    for(url <- urls) { 
      buffer.append(new File(url.getPath()));
      buffer.append(separator)
    }
    val withEnd = buffer.toString
    withEnd.substring(0, withEnd.indexOf(separator))
  }*/
  val javapath = System.getProperty("java.home") + separator + 
    		     "bin" + separator + "java"
  
  def createJVM = {
    val processBuilder = 
      new ProcessBuilder(javapath, "-cp",
      javapath, // classpath,
      classOf[CodeMonkey].getName()
      )
    val process = processBuilder.start()
    process.waitFor()
  }
  
  def setting = configs.getString("settings.classpath")
}