package com.gu.sangriascrooge

import com.gu.contentatom.thrift._
import sangria.schema._
import sangria.macros.derive.GraphQLOutputTypeLookup
import com.twitter.scrooge.ThriftStruct

object SangriaMacrosTest extends SangriaMacros[Unit] with App {
  val x = deriveThriftUnion[AtomData]
  //val obj = deriveThriftStruct[ChangeRecord]
}
