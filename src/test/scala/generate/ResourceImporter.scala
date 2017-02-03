package com.gu.sangriascrooge.generate

import com.twitter.scrooge.frontend.{ Importer, FileContents }

class ResourceImporter extends Importer {
  def apply(v1: String): Option[FileContents] = None
  def lastModified(fname: String): Option[Long] = None
}
