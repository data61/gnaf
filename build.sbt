import ReleaseTransformations._
import com.typesafe.sbt.license.{DepModuleInfo, LicenseInfo}

// concurrentRestrictions in Global := Seq(
//   Tags.limit(Tags.Test, 1) // only one test at a time across all projects because different tests write database & Lucene indices to same path
// )

// default release process, but without publishArtifacts
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

def hasPrefix(org: String, prefixes: Seq[String]) = prefixes.exists(x => org.startsWith(x))

lazy val commonSettings = Seq(
  organization := "au.csiro.data61.gnaf",
  // version := "0.1-SNAPSHOT", // see version.sbt maintained by sbt-release plugin
  licenses := Seq("BSD" -> url("https://github.com/data61/gnaf/blob/master/LICENSE.txt")),
  homepage := Some(url("https://github.com/data61/gnaf")),

  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-optimise"),
  exportJars := true, // required by sbt-onejar
  autoAPIMappings := true, // scaladoc
  
  unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil, // only Scala sources, no Java
  unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil,
  
  filterScalaLibrary := false, // sbt-dependency-graph: include scala library in output
  scalacOptions in (Compile,doc) := Seq("-diagrams"), // sbt-dependency-graph needs: sudo apt-get install graphviz
  
  EclipseKeys.withSource := true,
  // If Eclipse and sbt are both building to same dirs at same time it takes forever and produces corrupted builds.
  // So here we tell Eclipse to build somewhere else (bin is it's default build output folder)
  EclipseKeys.eclipseOutput in Compile := Some("bin"),   // default is sbt's target/scala-2.11/classes
  EclipseKeys.eclipseOutput in Test := Some("test-bin"), // default is sbt's target/scala-2.11/test-classes

  licenseOverrides := {
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("org.apache", "com.fasterxml", "com.google.guava", "org.javassist", "io.swagger", "org.json4s")) => LicenseInfo(LicenseCategory.Apache, "The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("com.thoughtworks.paranamer")) => LicenseInfo(LicenseCategory.BSD, "BSD-Style", "http://www.opensource.org/licenses/bsd-license.php")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("javax.ws.rs", "org.jvnet.mimepull", "org.glassfish")) => LicenseInfo(LicenseCategory.GPLClasspath, "CDDL + GPLv2 with classpath exception", "https://glassfish.dev.java.net/nonav/public/CDDL+GPL.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("ch.qos.logback")) => LicenseInfo(LicenseCategory.LGPL, "EPL + GNU Lesser General Public License", "http://logback.qos.ch/license.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("com.google.code.findbugs")) => LicenseInfo(LicenseCategory.LGPL, "GNU Lesser General Public License", "http://www.gnu.org/licenses/lgpl.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("org.slf4j")) => LicenseInfo(LicenseCategory.MIT, "MIT License", "http://www.slf4j.org/license.html")
    }
  )

lazy val gnafUtil = (project in file("gnaf-util")).
  settings(commonSettings: _*)

lazy val gnafDb = (project in file("gnaf-db")).
  dependsOn(gnafUtil).
  settings(commonSettings: _*)

lazy val gnafIndexer = (project in file("gnaf-indexer")).
  dependsOn(gnafUtil, gnafDb).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*).
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.indexer.Indexer")
  )
  
lazy val gnafLuceneUtil = (project in file("gnaf-lucene-util")).
  dependsOn(gnafUtil).
  settings(commonSettings: _*)
  
lazy val gnafLuceneIndexer = (project in file("gnaf-lucene-indexer")).
  dependsOn(gnafLuceneUtil).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*).
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.lucene.indexer.LuceneIndexer")
  )
  
lazy val gnafLuceneService = (project in file("gnaf-lucene-service")).
  dependsOn(gnafLuceneUtil).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*).
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.lucene.service.LuceneService")
  )

lazy val gnafTest = (project in file("gnaf-test")).
  dependsOn(gnafDb).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*).
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.test.Main")
  )
  
lazy val gnafDbService = (project in file("gnaf-db-service")).
  dependsOn(gnafDb).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*).
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.db.service.DbService")
  )

lazy val gnafContribService = (project in file("gnaf-contrib-service")).
  dependsOn(gnafUtil).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*).
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.contrib.service.ContribService")
  )
