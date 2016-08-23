name := "gnaf-db-service"

libraryDependencies ++= Seq(
  "ch.megard" %% "akka-http-cors" % "0.1.2",
  "au.csiro.data61.gnaf" %% "gnaf-db" % "0.1-SNAPSHOT",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.7.0", // adding swagger brings in all the horrible old javax.ws & Jackson dependencies!
  "io.swagger" % "swagger-annotations" % "1.5.9"
  )

libraryDependencies ++= Seq(
  "akka-actor",
  "akka-stream",
  "akka-http-experimental",
  "akka-http-spray-json-experimental",
  "akka-http-testkit"
  ) map ("com.typesafe.akka" %% _ % "2.4.3")
 