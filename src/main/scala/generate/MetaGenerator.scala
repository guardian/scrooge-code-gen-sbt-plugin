package com.gu.thrifttransformer.generate

import com.twitter.scrooge.frontend.ResolvedDocument
import scala.collection.immutable.SortedSet
import com.twitter.scrooge.ast._
import com.twitter.scrooge.{ast => scroogeAst}

import scala.collection.immutable.{Seq => ImmutableSeq}

case class Identifier(name: String) {
  val shouldBeQuoted = Identifier.keywords.contains(name)
  require(name.matches("^[A-Za-z_.]+"))
  val generate: String = if(shouldBeQuoted) s"""`$name`""" else name // possibly quote, just in case its a reserved word
}

object Identifier {
  val keywords = Seq("type")
}

sealed trait GeneratedCode {
  def generate: String
}

sealed trait GeneratedDefinition extends GeneratedCode {
  def name: Identifier
}

trait MetaGenerator {
  def generate(doc: Document): String
}

sealed abstract class ScalaType(name: String) {
  override def toString = name
}

object ScalaType {
  case object Boolean extends ScalaType("Boolean")
  case object String  extends ScalaType("String")
  case object Int     extends ScalaType("Int")
  case object Short   extends ScalaType("Short")
  case object Long    extends ScalaType("Long")
  case object Double  extends ScalaType("Double")
  case object Byte    extends ScalaType("Byte")

  case class List(fieldType: ScalaType) extends ScalaType(s"Seq[${fieldType}]")

  // this describes a field who's type is a custom type (e.g. a
  // struct) that is defined elsewhere in the document
  case class CustomType(name: Identifier) extends ScalaType(name.generate)
}

case class GeneratedField(name: Identifier, scalaType: ScalaType, fieldId: Int) extends GeneratedCode {
  val generate = s"${name.generate}: Option[${scalaType}] = None"
}

object GeneratedField {
  /* sort fields by thier numeric field id */
  implicit val ordering = Ordering.by[GeneratedField, Int](_.fieldId)
}

case class GeneratedCaseClass(name: Identifier, fields: SortedSet[GeneratedField]) extends GeneratedDefinition {
  val fieldsString = fields.map(_.generate).mkString(",")
  val generate = s"case class ${name.generate}($fieldsString)"
}

case class GeneratedPackage(definitions: Set[GeneratedDefinition], name: Option[Identifier] = None) extends GeneratedCode {
  val definitionsString = definitions.map(_.generate).mkString("\n")
  // add definitions to this package
  def `+`(newDefinition: GeneratedDefinition): GeneratedPackage =
      this.copy(definitions = definitions + newDefinition)
  def `++`(newDefinitions: Seq[GeneratedDefinition]): GeneratedPackage =
      this.copy(definitions = definitions ++ newDefinitions)
  def `++`(otherPkg: GeneratedPackage): GeneratedPackage =
      this.copy(definitions = definitions ++ otherPkg.definitions)
  val generate = {
    val packageDecl = name.map(n => s"package ${n.generate}\n").getOrElse("")
    s"$packageDecl$definitionsString"
  }
}

class CaseClassGenerator(val packageName: Identifier) {

  def genType(t: FunctionType): ScalaType = t match {
      case TBool => ScalaType.Boolean
      case TByte => ScalaType.Byte
      case TI16 => ScalaType.Short
      case TI32 => ScalaType.Int
      case TI64 => ScalaType.Long
      case TDouble => ScalaType.Double
      case TString => ScalaType.String
      case ListType(elementType, _) => ScalaType.List(genType(elementType))
      case StructType(st, _) => ScalaType.CustomType(Identifier(st.sid.name))
      case EnumType(enum, _) => ScalaType.CustomType(Identifier(enum.sid.name))
      case _ => throw new IllegalArgumentException(s"Unrecognised type $t")
    }

  def generateField(field: scroogeAst.Field): GeneratedField =
    GeneratedField(name = Identifier(field.originalName),
      scalaType = genType(field.fieldType),
      fieldId = field.index)

  def generateMembers(st: StructLike): SortedSet[GeneratedField] =
    SortedSet(st.fields.map(generateField):_*)

  /* a struct may have other structs within it, which means that we
   * may in fact need to generate more than one case class from this
   * definition. Therefore, we return a map of case classes, keyed off
   * the name, and these can then be merged together */
  def generateCaseClass(st: StructLike): GeneratedCaseClass =
    GeneratedCaseClass(Identifier(st.sid.name), generateMembers(st))

  def generateDefinitions(doc: ResolvedDocument, recurse: Boolean): Set[GeneratedDefinition] = {
    val local = doc.document.defs collect {
        case st: StructLike => generateCaseClass(st)
      }
    (local ++ (if(recurse) {
      (doc.document.headers collect {
        case Include(_, includedDoc) => generateDefinitions(doc.resolver(includedDoc), recurse)
      }).flatten
    } else Nil)).toSet
  }

  def generatePackage(rdoc: ResolvedDocument, recurse: Boolean = false): Seq[GeneratedPackage] = {
    val packageName = (rdoc.document.namespace("scala") orElse rdoc.document.namespace("java"))
      .map(id => Identifier(id.fullName))
    val includedDocs = if(recurse) {
        rdoc.document.headers.collect {
          case Include(fname, includedDoc) => (fname, rdoc.resolver(includedDoc))
        }
      } else Nil
    val packages = GeneratedPackage(generateDefinitions(rdoc, recurse), packageName) +:
      includedDocs.flatMap { d =>
        val (fname, rdoc) = d
        val incpkgs = generatePackage(rdoc, recurse)
        val pkgnames = incpkgs.map(_.name.map(_.generate).getOrElse("<noname>")).mkString(",")
        println(s"[PMR 1424] For document $fname $pkgnames")
        incpkgs
      }
    // merge the packages so that each one appears only once (and
    // thereby removing duplicate entries, which would otherwise
    // render the file uncompilable)
    packages.foldLeft(Map.empty: Map[Identifier, GeneratedPackage]) { (acc, pkg) =>
      val key = pkg.name.getOrElse(Identifier("_root_"))
      val updatedValue = acc.get(key).map(_ ++ pkg).getOrElse(pkg)
      acc.updated(key, updatedValue)
    }.values.toSeq
  }
}
