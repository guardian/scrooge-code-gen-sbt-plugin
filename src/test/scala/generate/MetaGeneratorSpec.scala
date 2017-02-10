package com.gu.thrifttransformer.generate

import org.scalatest.{ FunSpec, Matchers }

import com.twitter.scrooge.frontend.{ ThriftParser, TypeResolver }
import com.twitter.scrooge.{ ast => scroogeAst }

class MetaGeneratorSpec extends FunSpec with Matchers {

  lazy val resolvedDocument =  {
    val parser = new ThriftParser(new ResourceImporter("/example-thrift"), false)
    val resolver = new TypeResolver()
    resolver(parser.parseFile("simple.thrift"))
  }

  lazy val document = resolvedDocument.document

  def findField(name: String, st: scroogeAst.StructLike): Option[scroogeAst.Field] =
    st.fields.find(_.originalName == name)

  def findStruct(name: String, doc: scroogeAst.Document = document): Option[scroogeAst.StructLike] =
    doc.structs.find(_.originalName == name)

  lazy val simpleStruct = findStruct("SimpleStruct").get
  lazy val nameField    = findField("name", simpleStruct).get
  lazy val ageField     = findField("age", simpleStruct).get

  lazy val generator = new CaseClassGenerator()

  describe("MetaGenerator") {
    it("should correctly choose field type") {
      generator.genType(nameField.fieldType) should be(ScalaType.String)
    }
    it("should correctly generate field") {
      generator.generateField(ageField) should matchPattern {
        case GeneratedField(Identifier("age"), ScalaType.Int) =>
      }
    }
    it("should correctly identify fields") {
      generator.generateCaseClass(simpleStruct) should matchPattern {
        case GeneratedCaseClass(
          Identifier("SimpleStruct"),
          Seq(GeneratedField(Identifier("name"), ScalaType.String),
            GeneratedField(Identifier("age"), ScalaType.Int))) =>
      }
    }
    it("should print (it shouldn't)") {
      println(generator.generatePackage(document).generate)
    }
  }
}
