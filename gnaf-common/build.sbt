name := "gnaf-common"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.h2database" % "h2" % "1.4.191" % "runtime",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )

libraryDependencies ++= Seq(
  "slick-codegen",
  "slick",
  "slick-hikaricp"
) map ("com.typesafe.slick" %% _ % "3.1.1")
