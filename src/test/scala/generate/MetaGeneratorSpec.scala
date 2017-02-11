package com.gu.thrifttransformer.generate

import scala.collection.immutable.SortedSet
import org.scalatest.{ FunSpec, Matchers, Inside }

import com.twitter.scrooge.frontend.{ ThriftParser, TypeResolver }
import com.twitter.scrooge.{ ast => scroogeAst }

class MetaGeneratorSpec extends FunSpec with Matchers with Inside {

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
        case GeneratedField(Identifier("age"), ScalaType.Int, 2) =>
      }
    }
    it("should correctly identify fields") {
      inside(generator.generateCaseClass(simpleStruct)) {
        case GeneratedCaseClass(Identifier("SimpleStruct"), fields) =>
          fields should contain inOrderOnly (
            GeneratedField(Identifier("name"), ScalaType.String, 1),
            GeneratedField(Identifier("age"), ScalaType.Int, 2)
          )
      }
    }
    it("should print (it shouldn't)") {
      println(generator.generatePackage(Identifier("SimpleTest"), document).generate)
    }
  }
}
