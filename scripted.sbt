// this handles the configuration of the 'scripted' plugin, which is a tool for testing sbt plugins
// see: <http://www.scala-sbt.org/0.13/docs/Testing-sbt-plugins.html>

// use the sbt command `scripted` to run these tests

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
    Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false
