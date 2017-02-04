package com.gu.sangriascrooge.generate

import java.io.File
import com.twitter.scrooge.frontend.{ Importer, ThriftParser }

object Main {
  def main(args: Array[String]) { 
    val importer = Importer(args)
    //val fname = "/home/proberts/.ivy2/cache/com.gu/content-atom-model-thrift/jars/content-atom-model-thrift-2.4.31.jar"
    //val importer = new ZipImporter(new java.io.File(fname))
    //val importer = new ResourceImporter() +: new ResourceImporter("/atoms/")
    val thriftParser = new ThriftParser(importer, false)
    val doc = thriftParser.parseFile("contentatom.thrift")
    val generator = new SangriaScroogeGenerator
    doc.structs.headOption.foreach( st =>
      println(
        generator.generateStruct(st).syntax
      )
    )
  }
}
