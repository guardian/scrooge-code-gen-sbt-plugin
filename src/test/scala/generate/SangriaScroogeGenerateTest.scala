package com.gu.sangriascrooge.generate

import com.twitter.scrooge.frontend._

import java.io.InputStreamReader

object SangriaScroogeGenerateTest extends App {

  val parser = new ThriftParser(importer = NullImporter, strict = false)

  val thriftFile = "/contentatom.thrift"

  // find a thrift file on the class path
  val inputStream = this.getClass.getResourceAsStream(thriftFile)
  if(inputStream == null) throw new java.io.FileNotFoundException(thriftFile)
  val reader = new InputStreamReader(inputStream)
  val result = parser.parseAll(parser.document, reader)
}
