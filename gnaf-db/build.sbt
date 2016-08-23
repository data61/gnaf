name := "gnaf-db"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.191", // or postgres or whatever,  % "runtime" should be enough, but sbt slick.codegen needs it on compile classpath
  "au.csiro.data61.gnaf" %% "gnaf-util" % "0.1-SNAPSHOT"
  )

libraryDependencies ++= Seq(
  "slick-codegen", // only needed when generating slick mapping
  "slick",
  "slick-hikaricp"
) map ("com.typesafe.slick" %% _ % "3.1.1")
