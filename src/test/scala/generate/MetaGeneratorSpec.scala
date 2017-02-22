package com.gu.thrifttransformer.generate

import com.gu.thrifttransformer.sbt._
import scala.collection.immutable.SortedSet
import org.scalatest.{ FunSpec, Matchers, Inside, OptionValues }

import com.twitter.scrooge.frontend.{ ThriftParser, TypeResolver, DirImporter }
import com.twitter.scrooge.{ ast => scroogeAst }

class MetaGeneratorSpec extends FunSpec with Matchers with Inside with OptionValues {

  lazy val resolvedDocument =  {
    val parser = new ThriftParser(new DirImporter(
        new java.io.File(this.getClass.getResource("/example-thrift").getFile)), false
      )
    val resolver = new TypeResolver()
    resolver(parser.parseFile("simple.thrift"))
  }

  lazy val document = resolvedDocument.document

  def findField(name: String, st: scroogeAst.StructLike): Option[scroogeAst.Field] =
    st.fields.find(_.originalName == name)

  def findField(index: Int, st: scroogeAst.StructLike): Option[scroogeAst.Field] =
    st.fields.find(_.index == index)

  def findStruct(name: String, doc: scroogeAst.Document = document): Option[scroogeAst.StructLike] =
    doc.structs.find(_.originalName == name)

  lazy val simpleStruct = findStruct("SimpleStruct").get
  lazy val nameField    = findField("name", simpleStruct).get
  lazy val ageField     = findField("age", simpleStruct).get
  lazy val stringsField = findField(3, simpleStruct).get

  lazy val generator = new CaseClassGenerator(Identifier("SimpleTest"))

  describe("MetaGenerator") {
    it("should correctly choose field type") {
      generator.genType(nameField.fieldType) should be(ScalaType.String)
    }
    it("it should handle List type's field type") {
      generator.genType(stringsField.fieldType) should matchPattern {
        case ScalaType.List(ScalaType.String) =>
      }
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
            GeneratedField(Identifier("age"), ScalaType.Int, 2),
            GeneratedField(Identifier("strings"), ScalaType.List(ScalaType.String), 3)
          )
      }
    }
    it("it should generate package with multiple structs") {
      inside(generator.generatePackage(document)) {
        case GeneratedPackage(Identifier("SimpleTest"), caseClasses) =>
          caseClasses should have size 4
      }
    }
    it("it should handle nested structs") {
      inside(generator.generatePackage(document)) {
        case GeneratedPackage(Identifier("SimpleTest"), caseClasses) =>
          val nested = caseClasses.find(_.name == Identifier("HasNested")).value
          inside(nested) {
            case GeneratedCaseClass(Identifier("HasNested"), fields) =>
              fields.find(_.name == Identifier("nested")).value should matchPattern {
                case GeneratedField(Identifier("nested"),
                  ScalaType.CustomType(Identifier("SimpleStruct")), 2) =>
              }
          }
      }
    }
    it("should include data from included files") {
      inside(generator.generatePackage(document)) {
        case GeneratedPackage(_, caseClasses) =>
          caseClasses.find(_.name == Identifier("IncludedStruct")) shouldBe defined
      }
    }
  }
}
