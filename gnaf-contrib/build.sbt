name := "gnaf-contrib"

libraryDependencies ++= Seq(
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
