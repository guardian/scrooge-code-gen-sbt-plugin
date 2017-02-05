package com.gu.sangriascrooge.generate

import scala.meta._
import com.twitter.scrooge.ast.{ Field => ScroogeField, _ }
import sangria.schema._

class SangriaScroogeGenerator {

  val pkg = q"sangria.schema"

  def generateField(fld: ScroogeField) = {
    val graphqlType = fld.fieldType match {
      case TString =>
        q"""$pkg.StringType"""
      case any =>
        q"""$pkg.StringType"""
    }
    q"""sangria.schema.Field(name = ${fld.originalName}, $graphqlType)"""
  }

  def generateStruct(st: StructLike) = {
    val fields = st.fields.map(generateField(_))
    val fieldsList = q"List($fields)"
    q"""sangria.schema.ObjectType(name = ${st.originalName},
      fields = $fieldsList)"""
  }

  def generate(doc: Document) = ???

}
