package com.gu.sangriascrooge

import com.gu.contentatom.thrift._
import sangria.schema._
import sangria.macros.derive.GraphQLOutputTypeLookup
import GraphQLOutputTypeLookup._
import com.twitter.scrooge.ThriftStruct

object SangriaMacrosTest extends SangriaMacros[Unit] with App {
  //val tu = deriveThriftUnion[AtomData]

//
  val o = implicitly[GraphQLOutputTypeLookup[Option[Long]]]
 // val o = deriveThriftStruct[com.gu.contentatom.thrift.atom.recipe.RecipeAtom]
  //val x = implicitly[
    //_root_.sangria.schema.ObjectType[Unit, com.gu.contentatom.thrift.atom.recipe.RecipeAtom]
  //]
  //println(x)
  //val x = deriveOutputTypeLookupThriftUnion[AtomData]
  //println(x.graphqlType)
  //val obj = deriveThriftStruct[ChangeRecord]
}
