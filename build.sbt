
name := "Universal Tracker"

organization := "cm.utrackr"

version := "0.1"

scalaVersion := "2.11.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers ++= Seq(
  "ReactiveCouchbase Snapshots" at "https://raw.github.com/ReactiveCouchbase/repository/master/snapshots/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)



libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  val specs2V = "2.3.13"
  val logbackV = "1.1.2"
  Seq(
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing" % sprayV,
    "io.spray" %% "spray-json" % "1.3.1",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    /*   "org.slf4j"              %   "slf4j-nop"                   % "1.6.4", */
    "org.slf4j" % "slf4j-api" % "1.7.7",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "ch.qos.logback" % "logback-core" % logbackV,
    "ch.qos.logback" % "logback-classic" % logbackV,
    "com.github.t3hnar" %% "scala-bcrypt" % "2.4",
    "com.jason-goodwin" %% "authentikat-jwt" % "0.3.5",
    "com.github.nscala-time" %% "nscala-time" % "1.6.0",
    "com.timesprint" %% "hashids-scala" % "1.0.0",
    "org.reactivecouchbase" %% "reactivecouchbase-core" % "0.4-SNAPSHOT",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "org.specs2" %% "specs2-core" % specs2V % "test",
    "org.specs2" %% "specs2-matcher-extra" % specs2V % "test",
    "io.spray" %% "spray-testkit" % sprayV % "test"
  )
}

scalacOptions in Test ++= Seq("-Yrangepos")

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

Revolver.settings