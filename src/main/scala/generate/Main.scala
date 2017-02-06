package com.gu.sangriascrooge.generate
import org.scalafmt.Scalafmt
import java.io.File
import com.twitter.scrooge.frontend.{
  Importer,
  ThriftParser,
  TypeResolver
}

object Main {
  def main(args: Array[String]) { 
    val paths = Option(System.getenv("THRIFT_PATHS")).getOrElse("example-thrift")
    val importer = Importer(paths.split(":"))
    //val fname = "/home/proberts/.ivy2/cache/com.gu/content-atom-model-thrift/jars/content-atom-model-thrift-2.4.31.jar"
    //val importer = new ZipImporter(new java.io.File(fname))
    //val importer = new ResourceImporter() +: new ResourceImporter("/atoms/")
    val thriftParser = new ThriftParser(importer, false)
    //val doc = thriftParser.parseFile("simple.thrift")
    val doc = thriftParser.parseFile("contentatom.thrift")
    val resolvedDoc = TypeResolver()(doc)
    val srcCode = (new SangriaScroogeGenerator).generate("ContentAtom", resolvedDoc.document)
    println(
      Scalafmt.format(srcCode.syntax).get
    )
  }
}
