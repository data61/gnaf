name := "gnaf-db"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.193" // or postgres or whatever,  % "runtime" should be enough, but sbt slick.codegen needs it on compile classpath
  )

libraryDependencies ++= Seq(
  "slick-codegen", // only needed when generating slick mapping
  "slick",
  "slick-hikaricp"
) map ("com.typesafe.slick" %% _ % "3.1.1")
