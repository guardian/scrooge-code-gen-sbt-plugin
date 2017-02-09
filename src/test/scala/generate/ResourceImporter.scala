package com.gu.thrifttransformer.generate

import java.io.{ InputStream, InputStreamReader }

import com.twitter.scrooge.frontend.Importer

class ResourceImporter extends Importer {
  lazy val canonicalPaths: Seq[String] = Seq()

  private def readAll(inStream: InputStream): String = {
    val sb = new StringBuilder()
    val bufSize = 256
    val in = new InputStreamReader(inStream)
    val charBuffer = new Array[Char](bufSize)
    var count = 0
    while(count >= 0) {
      val count = in.read(charBuffer)
      if(count > 0)
        sb ++= charBuffer.take(count)
    }
    sb.toString
  }

  def apply(v1: String) = {
    Option(this.getClass.getResourceAsStream(v1)).map { in =>

    }
    None
  }
  def lastModified(filename: String): Option[Long] = None
}
