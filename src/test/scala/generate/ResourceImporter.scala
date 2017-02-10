package com.gu.thrifttransformer.generate

import java.io.{ InputStream, InputStreamReader }

import com.twitter.scrooge.frontend.{ FileContents, Importer }

class ResourceImporter(basePath: String = "") extends Importer {
  lazy val canonicalPaths: Seq[String] = Seq()

  private def readAll(inStream: InputStream): String = {
    val sb = new StringBuilder()
    val bufSize = 256
    val in = new InputStreamReader(inStream)
    val charBuffer = new Array[Char](bufSize)
    var count = 0
    while(count >= 0) {
      count = in.read(charBuffer)
      if(count > 0)
        sb ++= charBuffer.take(count)
    }
    sb.toString
  }

  def apply(v1: String) = {
    val fileName = basePath + "/" + v1
    Option(this.getClass.getResourceAsStream(fileName)).map { in =>
      FileContents(this, readAll(in), Some(v1))
    }
  }
  def lastModified(filename: String): Option[Long] = None
}
