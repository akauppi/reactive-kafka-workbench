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
  "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.9.1-SNAPSHOT",   // NOTE: "New API" coming in 0.9.1 (needs to be built & 'sbt publishLocal'ed locally, for now, from reactive-akka master) AKa190216
  //
  "org.scalatest" %% "scalatest" % "2.2.4" % Test
)
