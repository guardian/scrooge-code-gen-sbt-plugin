scalaVersion := "2.12.1"

thriftTransformPackageName := "testpackage"

libraryDependencies += "com.gu" % "content-atom-model-thrift" % "2.4.31"

resourceDirectories in Compile += baseDirectory.value / "src" / "main" / "thrift"

thriftTransformThriftFiles := Seq(
    file("simple.thrift"),
    file("contentatom.thrift")
  )
