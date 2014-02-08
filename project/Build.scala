import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "web-scala"
  val appVersion      = "0.1"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "org.scala-lang" % "scala-compiler" % "2.10.3",
    "com.scalatags" %% "scalatags" % "0.1.4",
    "org.scalatest" %% "scalatest" % "2.0.M8",
    "org.apache.directory.studio" % "org.apache.commons.codec" % "1.8",
    "javax.mail" % "javax.mail-api" % "1.5.0",
    "org.mindrot" % "jbcrypt" % "0.3m",

    "com.h2database" % "h2" % "1.3.172",
    
    "com.typesafe" % "config" % "1.0.2",
    "com.typesafe.akka" %% "akka-actor" % "2.2.1",
    "com.typesafe.akka" %% "akka-remote" % "2.2.1",
    "org.dupontmanual" %% "dm-image" % "0.1-SNAPSHOT",
    "org.dupontmanual" %% "scalajdo" % "0.2-SNAPSHOT",
    "org.dupontmanual" %% "dm-forms" % "0.2-SNAPSHOT"
  )

  val mySettings = Seq(
    scalaVersion := "2.10.3",
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-bootclasspath", "/usr/lib/jvm/java-6-oracle/jre/lib/rt.jar"),
    scalacOptions ++= Seq("-deprecation", "-feature"),
    resolvers ++= Seq("Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
                      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
                      "Java.net" at "http://download.java.net/maven/2/")
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    (mySettings ++ Nucleus.settings): _*)
}

object Nucleus {
  // defines our own ivy config that wont get packaged as part of your app
  // notice that it extends the Compile scope, so we inherit that classpath
  val Config = config("nucleus") extend Compile

  // our task
  val enhance = TaskKey[Unit]("enhance")

  val settings: Seq[Def.Setting[_]] = Seq(
    ivyConfigurations += Config,
    enhance <<= Seq(compile in Compile).dependOn,
    enhance in Config <<= (fullClasspath in Test, runner, streams) map { (cp, processRunner, str) =>
      val options = Seq("-v", "-pu", "webscala")
      val result = processRunner.run("org.datanucleus.enhancer.DataNucleusEnhancer", cp.files, options, str.log)
      result.foreach(sys.error)
    })
      
  /*def enhanceClasses(runner: ScalaRun, classpath: Seq[File], classes: File, streams: TaskStreams) = {
    val options = Seq("-v") ++ findAllClassesRecursively(classes).map(_.getAbsolutePath)
    val result = runner.run("org.datanucleus.enhancer.DataNucleusEnhancer", classpath, options, streams.log)
    result.foreach(sys.error)
  }
      
  def findAllClassesRecursively(dir: File): Seq[File] = {
    if (dir.isDirectory) {
      val files = dir.listFiles
      files.flatMap(findAllClassesRecursively(_))
    } else if (dir.getName.endsWith(".class")) {
      Seq(dir)
    } else {
      Seq.empty
    }
  }*/
}
