package com.gu.thrifttransformer.generate

import com.twitter.scrooge.ast.{ListType, NamedType, ReferenceType, _}
//import com.twitter.scrooge.backend.ScalaGeneratorFactory
import com.twitter.scrooge.frontend.ScroogeInternalException
//import com.twitter.scrooge.mustache.Dictionary.{apply => _, _}
import com.twitter.scrooge.{ast => scroogeAst}

import scala.meta._
import scala.collection.immutable.{Seq => ImmutableSeq}
import scala.meta.{Pat, Term, Tree}

trait MetaGenerator {
    def generate(doc: Document): Tree
}

class CaseClassGenerator() {

  def genType(t: FunctionType): Type = {
    val code = t match {
      case Void => t"Unit"
      case OnewayVoid => t"Unit"
      case TBool => t"Boolean"
      case TByte => t"Byte"
      case TI16 => t"Short"
      case TI32 => t"Int"
      case TI64 => t"Long"
      case TDouble => t"Double"
      case TString => t"String"
      case TBinary => t"ByteBuffer"
      case MapType(k, v, _) =>
        t"Map[${genType(k)}, ${genType(v)}]"
      case SetType(x, _) =>
        t"Set[${genType(x)}]"
      case ListType(x, _) =>
        t"Seq[${genType(x)}]"
//      case t: NamedType =>
//        val id = resolvedDoc.qualifyName(t, namespaceLanguage, defaultNamespace)
//        // Named types are capitalized.
//        genID(id.toTitleCase).toData
      case r: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should not appear in backend")
    }
    code
  }

  def generateField(field: scroogeAst.Field) = {
    val fieldName = Term.Name(field.originalName)
    val fieldType = genType(field.fieldType)
    param"""${fieldName}: ${fieldType}"""
  }

  def generateMembers(st: StructLike) = {
    val fields = st.fields.map(generateField)
    ImmutableSeq(fields:_*)
  }

  def generate(doc:Document): Seq[Tree] = {
    ImmutableSeq(doc.structs:_*).map{st =>
      val name = Type.Name(st.sid.name)
      val members = generateMembers(st)
      q"""case class ${name}(..${members})"""
    }
  }
}
