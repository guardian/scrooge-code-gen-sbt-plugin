lazy val commonSettings = Seq(
  scalaVersion := "2.12.1",
  libraryDependencies ++= Seq(
    "org.sangria-graphql" %% "sangria" % "1.0.0",
    "com.twitter" %% "scrooge-generator" % "4.13.0"
//    "org.apache.thrift" % "libthrift" % "0.10.0"
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .dependsOn(macros)
