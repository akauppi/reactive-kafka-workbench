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
  // We want to use the "New API" and it's only in '0.9.1-SNAPSHOT' (not '0.10.0' release). All this is VERY WEIRD
  // and confusing. Maybe we shouldn't use "reactive-kafka" at all, but do our own on top of Kafka Java API. AKa250216
  //
  "com.softwaremill.reactivekafka" %% "reactive-kafka-core" % "0.10.0-SNAPSHOT",   // NOTE: 0.9.1-SNAPSHOT needs to be built & 'sbt publishLocal'ed locally
  //
  "org.scalatest" %% "scalatest" % "2.2.4" % Test
)
