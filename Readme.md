# Scrooge code gen sbt plugin

This SBT plugin will take a thrift definition and generate a case
class for each definition, with all optional fields.

## How to

For examples of how to use it see the tests in `src/sbt-test`, but in
summary:

Add the plugin to `plugins.sbt` in your project folder (assuming
`0.0.1` is the latest version):

```scala
addSbtPlugin("com.gu" % "thrift-transformer-sbt" % "0.0.1")
```

and then either put your thrift files in `src/main/thrift` or make
them available on the classpath (e.g: add a jar with the thrift files
to your `libraryDependencies`, see below for an example), and then
modifiy the setting `thriftTransformThriftFiles` to list the thrift
files to be processed. They will be processed recursively (that is,
included files will also be processed).

So for example, the content atom thrift can be processed like this:

```scala
libraryDependencies += "com.gu" % "content-atom-model-thrift" % "2.4.31"
thriftTransformThriftFiles := Seq(file("contentatom.thrift"))
```

This will read in `contentatom.thrift` and then recursively process
all of the definitions that it includes.

If you want to modify the namespace of the generated classes (which by
default matches the namespace that is defined in the thrift files)
then you can set `thriftTransformChangeNamespace` to a function and it
will be applied to each namespace. E.g:

```scala
thriftTransformChangeNamespace := { (orig: String) => orig.replaceFirst("^prefix.", "modified.") }
```

would change a namespace that begins with "prefix.\*" to "modified.\*"

## Testing

As well as the usual ScalaTest based tests in `src/test/scala`, this
being an SBT plugin, there are also some tests based on
the
[scripted](http://www.scala-sbt.org/0.13/docs/Testing-sbt-plugins.html) SBT
plugin testing framework, which basically sets up a number of SBT
projects in `src/sbt-test` and makes sure they compile.

To run these, just run the command `scripted` from SBT.

By default these will run with thier SBT output supressed (as it is
quite verbose) so that all you get is a success or failure message for
each test. If you would like to see this output (e.g. if you are
debugging a failure) then just modify the `scriptedBufferLog` setting
in the `scripted.sbt` file.

## TODO

+ [ ] Properly name the genreated thrift files so that they don't
  clash, and so that we can remove the otherwise unused
  `thriftTransformPackageName` setting
