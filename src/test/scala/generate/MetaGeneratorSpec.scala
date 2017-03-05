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

  val fileName = new java.io.File("simple.thrift")
  lazy val resolvedDocument = parseFile(fileName.getPath) // default resolved document

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

  lazy val generator = new CaseClassGenerator

  // returns the first instance of a field that matches identifier
  def findGeneratedField(id: Identifier, pkgs: Seq[GeneratedPackage]): Option[GeneratedField] =
    (for {
        pkg <- pkgs
        GeneratedCaseClass(_, fields, _) <- pkg.definitions
        field <- fields
        if field.name == id
      } yield field).headOption

  describe("MetaGenerator") {
    it("should correctly choose field type") {
      generator.genType(nameField.fieldType, Map.empty) should be(ScalaType.String)
    }
    it("should handle List type's field type") {
      generator.genType(stringsField.fieldType, Map.empty) should matchPattern {
        case ScalaType.List(ScalaType.String) =>
      }
    }
    it("should correctly generate field") {
      generator.generateField(ageField) should matchPattern {
        case GeneratedField(Identifier("age"), ScalaType.Int, 2) =>
      }
    }
    it("should correctly identify fields") {
      inside(generator.generateCaseClass(simpleStruct, fileName)) {
        case GeneratedCaseClass(Identifier("SimpleStruct"), fields, _) =>
          fields should contain inOrderOnly (
            GeneratedField(Identifier("name"), ScalaType.String, 1),
            GeneratedField(Identifier("age"), ScalaType.Int, 2),
            GeneratedField(Identifier("strings"), ScalaType.List(ScalaType.String), 3)
          )
      }
    }
    it("should generate package with multiple structs") {
      inside(generator.generatePackage(resolvedDocument, fileName)) {
        case Seq(GeneratedPackage(caseClasses, _)) =>
          caseClasses should have size 3
      }
    }
    it("should handle nested structs") {
      inside(generator.generatePackage(resolvedDocument, fileName).headOption) {
        case Some(GeneratedPackage(caseClasses, _)) =>
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
      inside(generator.generatePackage(parseFile("hasEnum.thrift"), fileName).headOption) {
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
      forAtLeast(1, generator.generatePackage(resolvedDocument, fileName, recurse = true)) { pkg =>
        pkg.definitions.find(_.name == Identifier("IncludedStruct")) shouldBe defined
      }
    }
    it("should only generate a definition once") {
      forAll(generator.generatePackage(resolvedDocument, fileName, recurse = true)) { pkg =>
        val names = pkg.definitions.map(_.name)
        names should contain theSameElementsAs names.toSet
      }
    }
    it("should honour the namespaces") {
      generator.generatePackage(resolvedDocument, fname = fileName, recurse = true).map(_.name) should contain only (
        Some(Identifier("simple.test")),
        Some(Identifier("simple.test.included")),
        None
      )
    }
    it("should correctly qualify refs to types from included files") {
      val field = findGeneratedField(Identifier("otherFile"),
          generator.generatePackage(resolvedDocument, fname = fileName, recurse = true)
        ).value
      field.scalaType should matchPattern {
        case ScalaType.CustomType(Identifier("simple.test.included.IncludedStruct")) =>
      }
    //   val pkgs = generator.generatePackage(resolvedDocument, fname = fileName, recurse = true)
    //   forExactly(1, pkgs) { pkg =>
    //     inside(pkg) {
    //       case p @ GeneratedPackage(defs, Some(Identifier("simple.test"))) =>
    //         forExactly(1, defs) { defn =>
    //           inside(defn) { case => GeneratedCaseClass(Identifer("HasNested", fields, _)
    //             inside(
    //     }).value
    //   inside(defn) {
    //     case GeneratedCaseClass(_, 
    // }
    }
  }
}
