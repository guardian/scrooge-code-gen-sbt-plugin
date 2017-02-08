package com.gu.sangriascrooge.generate
import org.scalafmt.Scalafmt
import java.io.PrintStream

import com.gu.scala.generate.CaseClassGenerator
import com.twitter.scrooge.frontend.{Importer, ThriftParser, TypeResolver}

object Main {
  def main(args: Array[String]) { 
    val output = args.headOption.map(new PrintStream(_)).getOrElse(System.out)
    val paths = Option(System.getenv("THRIFT_PATHS")).getOrElse("example-thrift")
    val importer = Importer(paths.split(":"))
    val thriftParser = new ThriftParser(importer, false)
    val doc = thriftParser.parseFile("contentatom.thrift")
    val resolvedDoc = TypeResolver()(doc)

    val srcCode = (new CaseClassGenerator).generate(resolvedDoc.document)


    output.println(
      Scalafmt.format(srcCode.syntax).get
    )
  }
}
