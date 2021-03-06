package com.gu.thrifttransformer.sbt

import java.nio.file.{ FileSystem, Path }
import java.io.{ InputStream, InputStreamReader, File }

import com.twitter.scrooge.frontend.{ FileContents, Importer, ZipImporter }

/**
  *  I need a layer that sits on top of the ZipImporter, and which
  *  collapses the file paths into a canonical path branched of the
  *  current file
  */

/**
  * Canonicalises a path against a basePath before passing it to the
  *  wrapped importer.  The root (in other words, the case when no
  *  change to the path is required) is expressed by no path at all.
  */
case class ImporterWithBasePath(
  delegatedImporter: Importer,
  fs: FileSystem,
  basePath: Option[Path] = None
) extends Importer {
  // returns a multi-importer that includes this one and a new
  // basePath, both using the same underlying importer
  def addPath(newPath: Path): Importer = this +: this.copy(basePath = Some(newPath))

  private def resolveFileName(inputFileName:String): (Importer, String) = {
    val filePath: Path = basePath match {
        case Some(basePath) => basePath.resolve(inputFileName).normalize
        case None => fs.getPath(inputFileName)
      }
    val parent = filePath.getParent
    val importer = if(parent != null) addPath(parent) else this
    (importer, filePath.toString)
  }

  def apply(fileName: String) = {
    val (importer, resolvedFileName) = resolveFileName(fileName)
    delegatedImporter(resolvedFileName).map(_.copy(importer = importer))
  }

  val canonicalPaths = Seq()
  def lastModified(fileName: String) = delegatedImporter.lastModified(resolveFileName(fileName)._2)
}
