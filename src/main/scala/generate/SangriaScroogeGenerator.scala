package com.gu.sangriascrooge.generate

import scala.meta._
import com.twitter.scrooge.{ ast => scroogeAst }//.{ Field => ScroogeField, ListType => ScroogeListType, Enum => ScroogeEnum, _ }
import sangria.schema._

class SangriaScroogeGenerator {

  val pkg = q"sangria.schema"

  def scroogeFieldTypeToGraphQl(fType: scroogeAst.FieldType): Term = fType match {
    case scroogeAst.TString =>
      q"""$pkg.StringType"""
    case scroogeAst.TI32 |
        scroogeAst.TI16 |
        scroogeAst.TI64 => q"""$pkg.IntType"""
    case scroogeAst.TDouble =>
      q"""$pkg.FloatType"""
    case scroogeAst.TBool =>
      q"""$pkg.BooleanType"""
    case scroogeAst.ListType(fType, _) =>
      val sangriaFieldType = scroogeFieldTypeToGraphQl(fType)
      q"""$pkg.ListType($sangriaFieldType, None)"""
    case scroogeAst.StructType(st, _) => generateStruct(st)
    case scroogeAst.EnumType(e, _) => generateEnum(e)
    case any =>
      println(s"Error: unknown field $any")
      q"""$pkg.StringType"""
  }

  def generateField(fld: scroogeAst.Field): Term.Apply = {
    val graphqlType = scroogeFieldTypeToGraphQl(fld.fieldType)
    q"""sangria.schema.Field(name = ${fld.originalName}, $graphqlType)"""
  }

  def generateStruct(st: scroogeAst.StructLike) = {
    // fields is a `collection.Seq`, and so not neccessarily an
    // `immutable.Seq`, and scala.meta requires the latter.
    val fields = scala.collection.immutable.Seq(st.fields:_*).map(generateField(_))
    q"""sangria.schema.ObjectType(name = ${st.originalName}, fields = List(..$fields))"""
  }

  def generateEnum(en: scroogeAst.Enum) = {
    val fields = scala.collection.immutable.Seq(en.values:_*).map { fld =>
      q"""$pkg.EnumValue[Int](name = fld.sid.toUpperCase.name, value = fld.value)"""
    }
    q"""sangria.schema.EnumType(name = ${en.sid.name}, values = Seq(..$fields))"""
  }

  def generate(documentName: String, doc: scroogeAst.Document) = {
    val objects = scala.collection.immutable.Seq(doc.structs:_*).map { st =>
      val name = Pat.Var.Term(Term.Name(st.sid.name)) //Pat.Var.Term()
      val defn = generateStruct(st)
      q"""val ${name} = $defn"""
    }
    q"""object ${Term.Name(documentName)} { ..$objects } """
  }

}
