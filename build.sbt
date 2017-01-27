lazy val commonSettings = Seq(
  scalaVersion := "2.12.1",
  libraryDependencies ++= Seq(
    "org.sangria-graphql" %% "sangria" % "1.0.0",
    "com.twitter" %% "scrooge-core" % "4.12.0")
)

lazy val macros = (project in file("macros"))
  .settings(commonSettings)
  .settings(libraryDependencies +=
    "com.gu" %% "content-atom-model" % "2.4.30" % "test")

lazy val root = (project in file("."))
  .settings(commonSettings)
  .dependsOn(macros)
