package com.gu.sangriascrooge.generate
import org.scalafmt.Scalafmt
import java.io.PrintStream

import com.gu.scala.generate.CaseClassGenerator
import com.twitter.scrooge.frontend.{Importer, ThriftParser, TypeResolver}

object Main {
  def main(args: Array[String]) {
//    val input = args.headOption.map(new PrintStream(_)).getOrElse(System.out)
    val output = args.tail.headOption.map(new PrintStream(_)).getOrElse(System.out)

    val importer = Importer("example-thrift")
    val thriftParser = new ThriftParser(importer, false)
    val doc = thriftParser.parseFile("simple.thrift")
    val resolvedDoc = TypeResolver()(doc)

    val srcCode = (new CaseClassGenerator).generate(resolvedDoc.document)

    srcCode.foreach(tr => output.println(Scalafmt.format(tr.syntax).get))
  }
}
