package com.gu.sangriascrooge.generate

import scala.meta._
import com.twitter.scrooge.ast._
import sangria.schema._

class SangriaScroogeGenerator {

  def generateField(fld: Field) = {
    q"""sangria.schema.Field[$tType]()"""
  }

  def generateStruct(st: StructLike) = {
    q"""sangria.schema.ObjectType(name = ${st.originalName},

    )"""
  }

  def generate(doc: Document) = ???

}
