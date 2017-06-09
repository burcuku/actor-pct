name := """actor-pct"""

organization := "com.pct"
version := "1.0"

scalaVersion := "2.12.1"
publishMavenStyle := true

resolvers ++= Seq("sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.miguno.akka" %% "akka-mock-scheduler" % "0.5.2-SNAPSHOT",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)
