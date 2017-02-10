package com.gu.thrifttransformer.generate

import org.scalatest._

import com.twitter.scrooge.frontend.ThriftParser
import com.twitter.scrooge.{ ast => scroogeAst }
class MetaGeneratorSpec extends FunSpec {

  lazy val document = {
    val parser = new ThriftParser(new ResourceImporter("/example-thrift"), false)
    parser.parseFile("simple.thrift")
  }

  def findField(name: String, st: scroogeAst.StructLike): Option[scroogeAst.Field] =
    st.fields.find(_.originalName == name)

  def findStruct(name: String, doc: scroogeAst.Document = document): Option[scroogeAst.StructLike] =
    doc.structs.find(_.originalName == name)

  describe("MetaGenerator") {
    it("should correctly choose field type") {
      findStruct("SimpleStruct")
    }
  }
}
