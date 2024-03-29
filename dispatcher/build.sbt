name := """actor.scheduler"""

organization := "org.mpisws.actortest"
version := "1.0"

scalaVersion := "2.12.0"
publishMavenStyle := true

isSnapshot := true

resolvers ++= Seq("sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.miguno.akka" %% "akka-mock-scheduler" % "0.5.1",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)
