name := "gnaf-common"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.191", // or postgres or whatever,  % "runtime" should be enough, but sbt slick.codegen needs it on compile classpath
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )

libraryDependencies ++= Seq(
  "slick-codegen", // only needed when generating slick mapping
  "slick",
  "slick-hikaricp"
) map ("com.typesafe.slick" %% _ % "3.1.1")
