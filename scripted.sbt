// this handles the configuration of the 'scripted' plugin, which is a tool for testing sbt plugins
// see: <http://www.scala-sbt.org/0.13/docs/Testing-sbt-plugins.html>

// use the sbt command `scripted` to run these tests

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
    Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

// by default this is `false`, which means hide the verbose output of
// the scripted tests (only shows output if there's an error) but for
// debugging you might wish to switch this off
scriptedBufferLog := true
