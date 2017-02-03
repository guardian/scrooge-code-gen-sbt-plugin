package com.gu.sangriascrooge

import com.twitter.scrooge.{ ThriftStruct, ThriftEnum, ThriftUnion }
import sangria.schema._
import com.gu.contentatom.thrift._
import sangria.macros.derive.GraphQLOutputTypeLookup

object Main extends App with Implicits[Unit] {
  import SangriaMacros._
  import GraphQLOutputTypeLookup._
  println(
    deriveThriftStruct[AtomData]
  )
}
