name := "gnaf"

organization := "au.com.data61"

// version := "0.1-SNAPSHOT" // see version.sbt maintained by sbt-release plugin

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-optimise")

scalacOptions in (Compile,doc) := Seq("-diagrams") // diagrams need: sudo apt-get install graphviz

autoAPIMappings := true

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil // only Scala sources, no Java

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

licenses := Seq("GPL v3" -> url("http://www.gnu.org/copyleft/gpl.html"))

// homepage := Some(url("https://github.inside.nicta.com.au/nbacon/social-watch"))

EclipseKeys.withSource := true

// If Eclipse and sbt are both building to same dirs at same time it takes forever and produces corrupted builds.
// So here we tell Eclipse to build somewhere else (bin is it's default build output folder)
EclipseKeys.eclipseOutput in Compile := Some("bin")   // default is sbt's target/scala-2.11/classes

EclipseKeys.eclipseOutput in Test := Some("test-bin") // default is sbt's target/scala-2.11/test-classes

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

// experiment with running a pre-compilation step
// http://stackoverflow.com/questions/7344477/adding-new-task-dependencies-to-built-in-sbt-tasks

com.github.retronym.SbtOneJar.oneJarSettings

mainClass in run in Compile := Some("au.com.data61.gnaf.indexer.Main")

filterScalaLibrary := false // sbt-dependency-graph: include scala library in output

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.7", // how to use scalaVersion?
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.2",
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.jsuereth" %% "scala-arm" % "2.0.0-M1",
  "com.h2database" % "h2" % "1.4.191",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )

libraryDependencies ++= Seq(
  "slick-codegen",
  "slick"
) map ("com.typesafe.slick" %% _ % "3.1.1")
 
libraryDependencies ++= Seq(
  "spray-can",
  "spray-routing"
) map ("io.spray" %% _ % "1.3.3")
 
libraryDependencies ++= Seq(
  "lucene-core",
  "lucene-queryparser",
  "lucene-analyzers-common"
//  "lucene-facet",
//  "lucene-highlighter",
//  "lucene-suggest"
  ) map ("org.apache.lucene" % _ % "5.5.0")
