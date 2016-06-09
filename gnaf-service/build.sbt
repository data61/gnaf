name := "gnaf-service"

libraryDependencies ++= Seq(
  // "com.github.scopt" %% "scopt" % "3.3.0",
  // "com.jsuereth" %% "scala-arm" % "2.0.0-M1",
  "ch.megard" %% "akka-http-cors" % "0.1.2",
  "au.csiro.data61.gnaf" %% "gnaf-common" % "0.1-SNAPSHOT"
  )

libraryDependencies ++= Seq(
  "akka-actor",
  "akka-stream",
  "akka-http-experimental",
  "akka-http-spray-json-experimental",
  "akka-http-testkit"
  ) map ("com.typesafe.akka" %% _ % "2.4.3")
 