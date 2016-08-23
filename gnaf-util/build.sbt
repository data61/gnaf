name := "gnaf-util"

libraryDependencies ++= Seq(
  "io.spray" %%  "spray-json" % "1.3.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  // "org.scala-lang" % "scala-reflect" % "2.11.8", // Multiple dependencies with the same organization/name but different versions. To avoid conflict, pick one version
  // "org.scala-lang.modules" %% "scala-xml" % "1.0.4" // as above
  )
