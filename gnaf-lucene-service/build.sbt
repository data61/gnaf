name := "gnaf-lucene-service"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.jsuereth" %% "scala-arm" % "2.0.0-M1",
  "au.csiro.data61.gnaf" %% "gnaf-lucene-util" % "0.1-SNAPSHOT"
  // "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.7.0", // adding swagger brings in all the horrible old javax.ws & Jackson dependencies!
  // "io.swagger" % "swagger-annotations" % "1.5.9"
  )

libraryDependencies ++= Seq(
  "lucene-queryparser"
) map ("org.apache.lucene" % _ % "6.1.0")