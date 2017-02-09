/* sbt plugin that will take a thrift file, and run it through a
 * transform of some kind, which will result in code that will be
 * added to the build */

package com.gu.thrifttransformer.sbt

import sbt._
import Keys._

object ThriftTransformerSBT extends AutoPlugin {
  val thriftTransformPackageName = settingKey[String]("package to which the generated code should belong")
}
