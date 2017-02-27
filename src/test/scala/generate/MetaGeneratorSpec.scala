package com.gu.thrifttransformer.generate

import com.gu.thrifttransformer.sbt._
import scala.collection.immutable.SortedSet
import org.scalatest.{ FunSpec, Matchers, Inside, OptionValues, Inspectors }

import com.twitter.scrooge.frontend.{ ThriftParser, TypeResolver, DirImporter }
import com.twitter.scrooge.{ ast => scroogeAst }

class MetaGeneratorSpec extends FunSpec
    with Matchers
    with Inside
    with OptionValues
    with Inspectors {

  val parser = new ThriftParser(new DirImporter(
      new java.io.File(this.getClass.getResource("/example-thrift").getFile)), false
    )

  def parseFile(fileName: String) = new TypeResolver()(parser.parseFile(fileName))

  lazy val resolvedDocument = parseFile("simple.thrift") // default resolved document

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
        case GeneratedCaseClass(Identifier("SimpleStruct"), fields, _) =>
          fields should contain inOrderOnly (
            GeneratedField(Identifier("name"), ScalaType.String, 1),
            GeneratedField(Identifier("age"), ScalaType.Int, 2),
            GeneratedField(Identifier("strings"), ScalaType.List(ScalaType.String), 3)
          )
      }
    }
    it("it should generate package with multiple structs") {
      inside(generator.generatePackage(resolvedDocument, None)) {
        case Seq(GeneratedPackage(caseClasses, _)) =>
          caseClasses should have size 3
      }
    }
    it("it should handle nested structs") {
      inside(generator.generatePackage(resolvedDocument, None).headOption) {
        case Some(GeneratedPackage(caseClasses,_)) =>
          val nested = caseClasses.find(_.name == Identifier("HasNested")).value
          inside(nested) {
            case GeneratedCaseClass(Identifier("HasNested"), fields, _) =>
              fields.find(_.name == Identifier("nested")).value should matchPattern {
                case GeneratedField(Identifier("nested"),
                  ScalaType.CustomType(Identifier("SimpleStruct")), 2) =>
              }
          }
      }
    }
    it("should correctly handle enum definitions") {
      inside(generator.generatePackage(parseFile("hasEnum.thrift"), None).headOption) {
        case Some(GeneratedPackage(defns, _)) =>
          defns should have size 1
          inside(defns.headOption) {
            case Some(GeneratedEnumeration(Identifier("TestEnum"), fields, _)) =>
              fields should contain only (
                GeneratedEnumField(Identifier("good"), 1),
                GeneratedEnumField(Identifier("bad"),  2)
              )
          }
      }
    }
    it("should include data from included files") {
      inside(generator.generatePackage(resolvedDocument, None, recurse = true).headOption) {
        case Some(GeneratedPackage(caseClasses, _)) =>
          caseClasses.find(_.name == Identifier("IncludedStruct")) shouldBe defined
      }
    }
    it("should only generate a definition once") {
      forAll(generator.generatePackage(resolvedDocument, None, recurse = true)) { pkg =>
        val names = pkg.definitions.map(_.name)
        names should contain theSameElementsAs names.toSet
      }
    }
    it("should honour the namespaces") {
      println(
        generator.generatePackage(resolvedDocument, None, recurse = true).map(_.generate).mkString("\n")
      )
      generator.generatePackage(resolvedDocument, None, recurse = true).map(_.name) should contain only (
        Some(Identifier("simple.test")),
        Some(Identifier("simple.test.included")),
        None
      )
    }
  }
}
