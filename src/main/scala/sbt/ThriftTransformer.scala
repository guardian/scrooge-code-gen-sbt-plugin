/* sbt plugin that will take a thrift file, and run it through a
 * transform of some kind, which will result in code that will be
 * added to the build */

package com.gu.thrifttransformer.sbt

import com.gu.thrifttransformer.generate._
import com.twitter.scrooge.frontend._

import sbt._
import Keys._

object ThriftTransformerSBT extends AutoPlugin {
  object autoImport {
    val thriftTransformPackageName = settingKey[String]("package to which the generated code should belong")
    val thriftTransformThriftDirs  = settingKey[Seq[File]]("directories to be search for thrift files")
    val thriftTransformThriftFiles = settingKey[Seq[File]]("files (from within the search path thriftTransformPackageName), from which code should be generated")

    val generateTransformedThrift  = taskKey[Seq[File]]("generate the requested code from the thrift file(s)")
  }
  import autoImport._

  override lazy val trigger = allRequirements
  override lazy val requires = empty
  override lazy val projectSettings = Seq(
    thriftTransformPackageName := "thrift_transformed",
    thriftTransformThriftDirs  := Seq(baseDirectory.value / "src" / "main" / "thrift"),
    thriftTransformThriftFiles := Seq(file("set_in_scala_repo")),
    generateTransformedThrift  := {
      val importer = Importer(thriftTransformThriftDirs.value.map(_.getCanonicalPath))
      val parser = new ThriftParser(importer, false)
      val resolver = new TypeResolver()
      val docs = thriftTransformThriftFiles.value.map { f =>
        resolver(parser.parseFile(f.getPath))
      }
      Seq()
    }
  )
}
