scalaVersion := "2.12.1"

thriftTransformPackageName := "testpackage"

libraryDependencies += "com.gu" % "content-atom-model-thrift" % "2.4.31"

thriftTransformThriftFiles := Seq(
    file("contentatom.thrift")
  )
