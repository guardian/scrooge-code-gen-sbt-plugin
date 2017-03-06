scalaVersion := "2.12.1"

thriftTransformPackageName := "testpackage"

thriftTransformChangeNamespace := { (orig: String) => orig.replaceFirst("^prefix.", "modified.") }

thriftTransformThriftFiles := Seq(file("simple.thrift"))
