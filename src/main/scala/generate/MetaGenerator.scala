package com.gu.thrifttransformer.generate

import com.twitter.scrooge.ast.{ListType, NamedType, ReferenceType, _}
import com.twitter.scrooge.{ast => scroogeAst}

import scala.collection.immutable.{Seq => ImmutableSeq}

trait MetaGenerator {
  def generate(doc: Document): String
}

object ScalaType extends Enumeration {
  val Boolean, String, Int, Short, Long, Double,
    Byte = Value
}

case class GeneratedField(name: String, scalaType: ScalaType.Value)
case class GeneratedCaseClass(name: String, fields: Seq[GeneratedField])

class CaseClassGenerator() {

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

  def generateField(field: scroogeAst.Field) = {
    val fieldName = field.originalName
    val fieldType = genType(field.fieldType).toString
    s"""${fieldName}: ${fieldType}"""
  }

  def generateMembers(st: StructLike) = {
    val fields = st.fields.map(generateField)
    ImmutableSeq(fields:_*)
  }

  def generate(packageName: String, doc:Document) = {
    val caseClasses = ImmutableSeq(doc.structs:_*).map{st =>
      val name = st.sid.name
      val members = generateMembers(st).mkString(",")
      s"""case class ${name}($members)"""
    }
    s"""package $packageName { ${caseClasses.mkString("\n")} }"""
  }
}
