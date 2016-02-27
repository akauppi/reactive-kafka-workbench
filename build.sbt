name := "reactive-kafka-workbench"

organization := "github.akauppi"

scalaVersion  := "2.11.7"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "utf8",
  "-feature",
  "-language", "postfixOps"
)

libraryDependencies ++= Seq(
  // We want to use the "New API" and it's only in snapshots (not stable versions, for now). 
  //
  "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.10.0-SNAPSHOT",
  //
  "org.scalatest" %% "scalatest" % "2.2.4" % Test
)
