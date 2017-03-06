Sonatype.sonatypeSettings

sbtPlugin := true

scalaVersion := "2.10.6"

organization := "com.gu"

name := "thrift-transformer-sbt"

libraryDependencies ++= Seq(
  "com.twitter"         %% "scrooge-generator"         % "4.13.0",
  "org.scalatest"       %% "scalatest"                 % "3.0.1"  % "test",
  "com.gu"               % "content-atom-model-thrift" % "2.4.31" % "test"
)

scmInfo := Some(ScmInfo(url("https://github.com/guardian/content-atom"),
                        "scm:git:git@github.com:guardian/content-atom.git"))

pomExtra := (
  <url>https://github.com/guardian/scrooge-code-gen-sbt-plugin</url>
)

licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

import ReleaseTransformations._

releaseProcess -= pushChanges
