// addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "2.1.0")

// I got it saying: Source code has generated in /home/neil/sw/gnaf/target/scala-2.11/src_managed/main/au/com/data61/gnaf/db/Tables.scala
// but this file was not actually created, so I'm giving up on this plugin for now. 
// addSbtPlugin("com.github.tototoshi" % "sbt-slick-codegen" % "1.2.0")

// required by above
// libraryDependencies += "com.h2database" % "h2" % "1.4.191"

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

addSbtPlugin("org.scala-sbt.plugins" % "sbt-onejar" % "0.8")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "1.1.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
