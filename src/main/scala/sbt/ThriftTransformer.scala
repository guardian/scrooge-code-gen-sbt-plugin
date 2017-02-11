/* sbt plugin that will take a thrift file, and run it through a
 * transform of some kind, which will result in code that will be
 * added to the build */

package com.gu.thrifttransformer.sbt

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
  override lazy val requires = sbt.plugins.JvmPlugin
  override lazy val buildSettings = Seq(
    thriftTransformPackageName := "ThriftTransformed",
    thriftTransformThriftDirs  := Seq(),
    thriftTransformThriftFiles := Seq(),
    generateTransformedThrift  := {
      println("transforming ...")
      Seq()
    }
  )
}
