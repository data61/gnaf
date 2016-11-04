name := "gnaf-contrib"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.193", // or postgres or whatever,  % "runtime" should be enough, but sbt slick.codegen needs it on compile classpath
  "ch.megard" %% "akka-http-cors" % "0.1.2",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.7.0", // adding swagger brings in all the horrible old javax.ws & Jackson dependencies!
  "io.swagger" % "swagger-annotations" % "1.5.9"
  )

libraryDependencies ++= Seq(
  "slick-codegen", // only needed when generating slick mapping
  "slick",
  "slick-hikaricp"
) map ("com.typesafe.slick" %% _ % "3.1.1")

libraryDependencies ++= Seq(
  "akka-actor",
  "akka-stream",
  "akka-http-experimental",
  "akka-http-spray-json-experimental",
  "akka-http-testkit"
  ) map ("com.typesafe.akka" %% _ % "2.4.3")

  