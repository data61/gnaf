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
  organization := "au.com.data61",
  // version := "0.1-SNAPSHOT", // see version.sbt maintained by sbt-release plugin
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-optimise"),
  scalacOptions in (Compile,doc) := Seq("-diagrams"), // sbt-dependency-graph needs: sudo apt-get install graphviz
  autoAPIMappings := true,
  filterScalaLibrary := false, // sbt-dependency-graph: include scala library in output
  unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil, // only Scala sources, no Java
  unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil,
  licenses := Seq("BSD" -> url("https://github.inside.nicta.com.au/nbacon/gnaf/blob/master/CSIRO%20BSD%20MIT%20Licence%20v2.0-4.txt")),
  homepage := Some(url("https://github.data61.csiro.au/nbacon/gnaf")),
  EclipseKeys.withSource := true,
  // If Eclipse and sbt are both building to same dirs at same time it takes forever and produces corrupted builds.
  // So here we tell Eclipse to build somewhere else (bin is it's default build output folder)
  EclipseKeys.eclipseOutput in Compile := Some("bin"),   // default is sbt's target/scala-2.11/classes
  EclipseKeys.eclipseOutput in Test := Some("test-bin"), // default is sbt's target/scala-2.11/test-classes
  EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
  licenseOverrides := {
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("org.apache", "com.fasterxml", "com.google.guava", "org.javassist")) => LicenseInfo(LicenseCategory.Apache, "The Apache Software License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0.txt")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("com.thoughtworks.paranamer")) => LicenseInfo(LicenseCategory.BSD, "BSD-Style", "http://www.opensource.org/licenses/bsd-license.php")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("javax.ws.rs", "org.jvnet.mimepull", "org.glassfish")) => LicenseInfo(LicenseCategory.GPLClasspath, "CDDL + GPLv2 with classpath exception", "https://glassfish.dev.java.net/nonav/public/CDDL+GPL.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("ch.qos.logback")) => LicenseInfo(LicenseCategory.LGPL, "EPL + GNU Lesser General Public License", "http://logback.qos.ch/license.html")
    case DepModuleInfo(org, _, _) if hasPrefix(org, Seq("org.slf4j")) => LicenseInfo(LicenseCategory.MIT, "MIT License", "http://www.slf4j.org/license.html")
    }
  )

lazy val root = (project in file(".")).
  aggregate(gnafCommon, gnafIndexer, gnafService).
  settings(
    unmanagedSourceDirectories in Compile := Nil, // no sources in root project
    unmanagedSourceDirectories in Test := Nil
  )

lazy val gnafCommon = (project in file("gnaf-common")).
  settings(commonSettings: _*).
  settings(
    // other settings
  )

lazy val gnafIndexer = (project in file("gnaf-indexer")).
  dependsOn(gnafCommon).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*). // `oneJar` task
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.indexer.Indexer")
  )
  
lazy val gnafService = (project in file("gnaf-service")).
  dependsOn(gnafCommon).
  settings(commonSettings: _*).
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*). // `oneJar` task
  settings(
    mainClass in run in Compile := Some("au.csiro.data61.gnaf.service.GnafService")
  )
 