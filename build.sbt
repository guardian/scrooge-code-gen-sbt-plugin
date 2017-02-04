lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  libraryDependencies ++= Seq(
    "org.sangria-graphql" %% "sangria"                   % "1.0.0",
    "com.twitter"         %% "scrooge-generator"         % "4.13.0",
    "org.scalameta"       %% "scalameta"                 % "1.4.0",
    "com.gu"               % "content-atom-model-thrift" % "2.4.31"
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings)
