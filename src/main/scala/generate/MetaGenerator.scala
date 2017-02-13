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

sealed trait GeneratedDefinition extends GeneratedCode

trait MetaGenerator {
  def generate(doc: Document): String
}

object ScalaType extends Enumeration {
  val Boolean, String, Int, Short, Long, Double,
    Byte = Value
}

case class GeneratedField(name: Identifier, scalaType: ScalaType.Value, fieldId: Int) extends GeneratedCode {
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

  def genType(t: FunctionType): ScalaType.Value = t match {
    case TBool => ScalaType.Boolean
    case TByte => ScalaType.Byte
    case TI16 => ScalaType.Short
    case TI32 => ScalaType.Int
    case TI64 => ScalaType.Long
    case TDouble => ScalaType.Double
    case TString => ScalaType.String
    case _ => throw new IllegalArgumentException()
  }

  def generateField(field: scroogeAst.Field): GeneratedField =
    GeneratedField(name = Identifier(field.originalName),
      scalaType = genType(field.fieldType),
      fieldId = field.index)

  def generateMembers(st: StructLike): SortedSet[GeneratedField] = SortedSet(st.fields.map(generateField):_*)

  def generateCaseClass(st: StructLike): GeneratedCaseClass = {
    val name = st.sid.name
    val members = generateMembers(st)
      GeneratedCaseClass(Identifier(name), members)
  }

  def generatePackage(doc:Document): GeneratedPackage = {
    val caseClasses = doc.structs.map(generateCaseClass)
    GeneratedPackage(packageName, caseClasses)

  }
}
