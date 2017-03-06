# Scrooge code gen sbt plugin

This SBT plugin will take a thrift definition and generate a case
class for each definition, with all optional fields.

For examples of how to use it see the tests in `src/sbt-test`, but in
summary: either put your thrift files in `src/main/thrift` or make
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

## TODO

+ [ ] Properly name the genreated thrift files so that they don't
  clash, and so that we can remove the otherwise unused
  `thriftTransformPackageName` setting
