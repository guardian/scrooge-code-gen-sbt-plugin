/* sbt plugin that will take a thrift file, and run it through a
 * transform of some kind, which will result in code that will be
 * added to the build */

package com.gu.thrifttransformer.sbt

import com.gu.thrifttransformer.generate._
import com.twitter.scrooge.frontend._
import java.io.PrintStream

import java.nio.file.FileSystems
import java.net.URI

import sbt._
import Keys._
import Classpaths.managedJars

object ThriftTransformerSBT extends AutoPlugin {
  object autoImport {
    val thriftTransformPackageName  = settingKey[String]("package to which the generated code should belong")
    val thriftTransformThriftDirs   = settingKey[Seq[File]]("directories to be search for thrift files")
    val thriftTransformThriftFiles  = settingKey[Seq[File]]("files (from within the search path thriftTransformPackageName), from which code should be generated")
    val thriftTransformSourceDir    = settingKey[File]("where will the generated source code be written to form part of the build?")
    val thriftTransformUseClassPath = settingKey[Boolean]("should we (also?) search in the class path when resolving thrift files?")
    val generateTransformedThrift   = taskKey[Seq[File]]("generate the requested code from the thrift file(s)")
  }
  import autoImport._

  // sourceGenerators gets reset when Jvm Plugin starts, so we want to start
  // after that
  // <http://stackoverflow.com/questions/24724406/how-to-generate-sources-in-an-sbt-plugin?rq=1>
  override lazy val requires = sbt.plugins.JvmPlugin
  override lazy val trigger = allRequirements
  override lazy val projectSettings = Seq(
    thriftTransformPackageName  := "thrift_transformed",
    thriftTransformThriftDirs   := Seq(baseDirectory.value / "src" / "main" / "thrift"),
    thriftTransformThriftFiles  := Seq(file("set_in_scala_repo")),
    thriftTransformSourceDir    := sourceManaged.value / "thriftTransform" / "src",
    thriftTransformUseClassPath := true,
    generateTransformedThrift   := {
      val classpathImporter = managedJars(Compile, classpathTypes.value, update.value)
        .map { jarFile =>
          val fname = jarFile.data
          val fs = FileSystems.newFileSystem(URI.create(s"jar:file://$fname"), new java.util.HashMap[String, Any]())
          ImporterWithBasePath(new ZipImporter(fname), fs.getPath(""))
          //NullImporter
        }
        .foldLeft(NullImporter: Importer)((acc, i) => i +: acc)
      val importer = Importer(thriftTransformThriftDirs.value.map(_.getCanonicalPath)) +: classpathImporter
      val parser = new ThriftParser(importer, false)
      val resolver = new TypeResolver()
      val generator = new CaseClassGenerator(Identifier(thriftTransformPackageName.value))
      val docs = thriftTransformThriftFiles.value.map { f =>
        resolver(parser.parseFile(f.getPath))
      }
      val srcFiles = docs.map(resolvedDoc => generator.generatePackage(resolvedDoc.document))
      // write each document out to a file, returning the filename (as File())
      srcFiles.map { srcFile =>
        val fname = thriftTransformSourceDir.value / (thriftTransformPackageName.value + ".scala")
        fname.getParentFile.mkdirs()
        val out = new PrintStream(fname)
        out.print(srcFile.generate)
        out.close()
        fname
      }
    },
    sourceGenerators in Compile += generateTransformedThrift.taskValue
  )
}
