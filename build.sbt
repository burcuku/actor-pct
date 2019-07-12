ThisBuild / organization := "org.mpisws.actortest"
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.12.0"
ThisBuild / publishMavenStyle := true
ThisBuild / isSnapshot := true
lazy val root = (project in file("."))
  .settings(
    name := "actor-testing"
  )
  .aggregate(
    dispatcher,
    explorer,
    explorerProtocol
  )

lazy val dispatcher = (project in file("./dispatcher"))
  .settings(
    name := "dispatcher",
    settings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.14",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % "test",
      "org.scalatest" %% "scalatest" % "3.0.0" % "test",
      "com.miguno.akka" %% "akka-mock-scheduler" % "0.5.1",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  ).dependsOn(explorerProtocol)

lazy val explorer = (project in file("./explorer"))
  .settings(
    name := "explorer",
    mainClass in Compile := Some("Main"),
    settings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.14",
      "org.scalatest" %% "scalatest" % "3.0.0" % "test",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.0"
    )
  ).dependsOn(explorerProtocol)



lazy val explorerProtocol = project.in(file("./explorer-protocol"))
  .settings(
    name := "explorer-protocol",
    settings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.14",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.0",
      "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % "test"
    )
  )


lazy val settings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  ),
  publishArtifact in(Compile, packageDoc) := false
)

lazy val compilerOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "encoding", "utf8"
)
