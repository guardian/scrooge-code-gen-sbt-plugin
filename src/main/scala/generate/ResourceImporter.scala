package com.gu.sangriascrooge.generate

import java.net.File
import scala.io.Source
import com.twitter.scrooge.frontend.{ Importer, FileContents }

/* this is a simple implmementation of the `Importer` interface
 * belonging to Scrooge, which will allow it to find a file from the
 * class path. */

class ResourceImporter(basePath: File = new File("/")) extends Importer {
  private lazy val cl = this.getClass

  /* Option(x) returns None if x is null */
  private def resource(path: File) = { println(s"$basePath$path"); Option(cl.getResource(s"$basePath$path")) }

  lazy val canonicalPaths = Seq()
  def apply(path: String): Option[FileContents] = {
    println(s"[PMR ] 16:23 ${path}")
    resource(path) map { r => 
      val sb = new StringBuilder()
      val in = Source.fromInputStream(r.openStream())
      in.foreach(sb += _)
      FileContents(importer = this, data = sb.toString, thriftFilename = Some(r.toString))
    }
  }

  def lastModified(fname: String): Option[Long] = None
}
