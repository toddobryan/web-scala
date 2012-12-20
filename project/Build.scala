import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "web-scala"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "org.scala-lang" % "scala-compiler" % "2.10.0-RC1",
    "org.scala-lang" % "scala-swing" % "2.10.0-RC1",
    "org.scala-lang" % "scala-actors" % "2.10.0-RC1",
    "org.scalatest" % "scalatest_2.10.0-RC1" % "2.0.M4" % "test",
    "commons-codec" % "commons-codec" % "1.6"    
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
    scalacOptions ++= Seq("-deprecation", "-feature")
  )

}
