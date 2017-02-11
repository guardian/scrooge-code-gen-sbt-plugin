sbtPlugin := true

scalaVersion := "2.10.6"

organization := "com.gu"

name := "thrift-transformer-sbt"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.twitter"         %% "scrooge-generator"         % "4.13.0",
  "com.gu"               % "content-atom-model-thrift" % "2.4.31",
  "org.scalatest"       %% "scalatest"                 % "3.0.1" % "test"
)
