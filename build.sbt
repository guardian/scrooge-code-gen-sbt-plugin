sbtPlugin := true

lazy val commonSettings = Seq(
  scalaVersion := "2.10.6",
  libraryDependencies ++= Seq(
    "com.twitter"         %% "scrooge-generator"         % "4.13.0",
    "com.gu"               % "content-atom-model-thrift" % "2.4.31",
    "org.scalatest"       %% "scalatest"                 % "3.0.1" % "test"
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings)
