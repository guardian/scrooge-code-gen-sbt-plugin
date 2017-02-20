package com.gu.thrifttransformer.generate

import scala.collection.immutable.SortedSet
import com.twitter.scrooge.ast.{ListType, NamedType, ReferenceType, _}
import com.twitter.scrooge.{ast => scroogeAst}

import scala.collection.immutable.{Seq => ImmutableSeq}

case class Identifier(generate: String) {
  assert(generate.matches("^[A-Za-z_]+"))
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

  case class  CaseClass(name: Identifier) extends ScalaType(name.generate)
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

case class GeneratedPackage(name: Identifier, definitions: Seq[GeneratedDefinition]) extends GeneratedCode {
  val definitionsString = definitions.map(_.generate).mkString("\n")
  val generate = s"package ${name.generate} { $definitionsString }"
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
      case StructType(st, _) => ScalaType.CaseClass(Identifier(st.sid.name))
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

  def generatePackage(doc:Document): GeneratedPackage =
    GeneratedPackage(packageName, doc.structs.map(generateCaseClass _))
}
