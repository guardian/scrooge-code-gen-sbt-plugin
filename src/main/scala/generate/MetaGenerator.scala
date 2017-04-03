package com.gu.thrifttransformer.generate

import com.twitter.scrooge.frontend.ResolvedDocument
import java.io.File
import scala.collection.immutable.SortedSet
//import com.twitter.scrooge.ast._
import com.twitter.scrooge.{ast => scroogeAst}

import scala.collection.immutable.{Seq => ImmutableSeq}

trait TypeIdentifier { def generate: String }
case class Identifier(name: String) extends TypeIdentifier {
  val shouldBeQuoted = Identifier.keywords.contains(name)
  require(name.matches("^[A-Za-z_.]+"))
  val generate: String = if(shouldBeQuoted) s"""`$name`""" else name // possibly quote, just in case its a reserved word
}

case class EnumValueIdentifier(name: Identifier) extends TypeIdentifier {
  val generate: String = name.generate
}

object Identifier {
  val keywords = Seq("type")
}

sealed trait GeneratedCode {
  def generate: String
}

sealed trait GeneratedDefinition extends GeneratedCode {
  def definedIn: File
  def name: Identifier
}

trait MetaGenerator {
  def generate(doc: scroogeAst.Document): String
}

sealed abstract class ScalaType(name: String) {
  override def toString = name
}

object ScalaType {
  case object Boolean extends ScalaType("Boolean")
  case object String  extends ScalaType("String")
  case object Int     extends ScalaType("Int")
  case object Short   extends ScalaType("Short")
  case object Long    extends ScalaType("Long")
  case object Double  extends ScalaType("Double")
  case object Byte    extends ScalaType("Byte")

  case class List(fieldType: ScalaType) extends ScalaType(s"Seq[${fieldType}]")

  // this describes a field who's type is a custom type (e.g. a
  // struct) that is defined elsewhere in the document
  case class CustomType(name: TypeIdentifier) extends ScalaType(name.generate)
}

case class GeneratedField(name: Identifier, scalaType: ScalaType, fieldId: Int) extends GeneratedCode {
  val generate = s"${name.generate}: Option[${scalaType}] = None"
}

object GeneratedField {
  /* sort fields by thier numeric field id */
  implicit val ordering = Ordering.by[GeneratedField, Int](_.fieldId)
}

case class GeneratedCaseClass(
  name: Identifier,
  fields: SortedSet[GeneratedField],
  definedIn: File
) extends GeneratedDefinition {
  val fieldsString = fields.map(_.generate).mkString(",")
  val generate = s"case class ${name.generate}($fieldsString)"
}

// if constant is missing, it will be autogenerated
case class GeneratedEnumField(name: Identifier, constant: Int, parent: Identifier) {
  lazy val generate: String = {
    s"""case object ${name.generate} extends ${parent.generate} {val identifier = ${constant}}"""
  }
}
object GeneratedEnumField {
  implicit val ordering = Ordering.by[GeneratedEnumField, Int](_.constant)
}
case class GeneratedEnumeration(
  name: Identifier,
  fields: SortedSet[GeneratedEnumField],
  definedIn: File
) extends GeneratedDefinition {
  lazy val fieldStr = fields.toSeq.map(f => f.generate)
  lazy val generate =
    s"""sealed trait ${name.generate} {val identifier: Int} \n
       | object ${name.generate} { \n
       | ${fieldStr.mkString("; ")} }""".stripMargin
}
case class GeneratedPackage(
  definitions: Set[GeneratedDefinition],
  name: Option[Identifier] = None
) extends GeneratedCode {
  val definitionsString = definitions.map(_.generate).mkString("\n")
  // add definitions to this package
  def `+`(newDefinition: GeneratedDefinition): GeneratedPackage =
      this.copy(definitions = definitions + newDefinition)
  def `++`(newDefinitions: Seq[GeneratedDefinition]): GeneratedPackage =
      this.copy(definitions = definitions ++ newDefinitions)
  def `++`(otherPkg: GeneratedPackage): GeneratedPackage =
      this.copy(definitions = definitions ++ otherPkg.definitions)
  val generate = {
    val packageDecl = name.map(n => s"package ${n.generate}\n").getOrElse("")
    s"$packageDecl$definitionsString"
  }
}

/*
 * transformNamespace can be used to transform the package name once
 * it is generated
 */
class CaseClassGenerator(transformNamespace: String => String = identity) {

  type IncludedTypesMap = Map[String, Option[String]]

  def genType(t: scroogeAst.FunctionType, includedMap: IncludedTypesMap): ScalaType = t match {
      case scroogeAst.TBool => ScalaType.Boolean
      case scroogeAst.TByte => ScalaType.Byte
      case scroogeAst.TI16 => ScalaType.Short
      case scroogeAst.TI32 => ScalaType.Int
      case scroogeAst.TI64 => ScalaType.Long
      case scroogeAst.TDouble => ScalaType.Double
      case scroogeAst.TString => ScalaType.String
      case scroogeAst.ListType(elementType, _) => ScalaType.List(genType(elementType, includedMap))
      case scroogeAst.StructType(st, scoped) =>
        val typeName = scoped.map { scope =>
            // should fail if we do have a scope but it isn't the map,
            // because that would mean we don't actually have a
            // defintion of this struct
            includedMap.get(scope.name) match {
              case Some(Some(nm)) => nm + "." + st.sid.name
              case Some(None) => st.sid.name
              case None => throw new IllegalArgumentException(
                s"Missing include file referenced as ${scope.name}, from " + includedMap.keys.mkString(", ")
              )
            }
          } getOrElse st.sid.name
        ScalaType.CustomType(Identifier(typeName))
      case scroogeAst.EnumType(enum, _) => ScalaType.CustomType(EnumValueIdentifier(Identifier(enum.sid.name)))
      case _ => throw new IllegalArgumentException(s"Unrecognised type $t")
    }

  def generateField(field: scroogeAst.Field, includeMap: IncludedTypesMap = Map.empty): GeneratedField =
    GeneratedField(name = Identifier(field.originalName),
      scalaType = genType(field.fieldType, includeMap),
      fieldId = field.index)

  def generateMembers(st: scroogeAst.StructLike, includeMap: IncludedTypesMap): SortedSet[GeneratedField] =
    SortedSet(st.fields.map(fld => generateField(fld, includeMap)):_*)

  /* a struct may have other structs within it, which means that we
   * may in fact need to generate more than one case class from this
   * definition. Therefore, we return a map of case classes, keyed off
   * the name, and these can then be merged together */
  def generateCaseClass(st: scroogeAst.StructLike, fname: File, includeMap: IncludedTypesMap = Map.empty) =
    GeneratedCaseClass(Identifier(st.sid.name), generateMembers(st, includeMap), fname)

  def generateDefinition(fname: File, includeMap: IncludedTypesMap):
      PartialFunction[scroogeAst.Definition, GeneratedDefinition] = {
    case st: scroogeAst.StructLike => generateCaseClass(st, fname, includeMap)
    case scroogeAst.Enum(name, values, _, _) =>
      GeneratedEnumeration(Identifier(name.fullName),
        SortedSet(values .map(f =>
          GeneratedEnumField(Identifier(f.sid.fullName), f.value, Identifier(name.fullName))
        ): _*), fname)
  }

  def generateDefinitions(doc: ResolvedDocument, recurse: Boolean, fname: File,
    includeMap: IncludedTypesMap): Set[GeneratedDefinition] =
    doc.document.defs.collect(generateDefinition(fname, includeMap)).toSet

  def docPackageName(doc: scroogeAst.Document): Option[Identifier] =
    (doc.namespace("scala") orElse doc.namespace("java")).map(
      id => Identifier(transformNamespace(id.fullName)) // applies the namespace transformer if present
    )

  case class IncludedFileDetails(fname: String, doc: ResolvedDocument, namespace: Option[Identifier])

  def generatePackage(rdoc: ResolvedDocument, fname: File, recurse: Boolean = false): Seq[GeneratedPackage] = {
    val includedDocs =
        rdoc.document.headers.collect {
          case scroogeAst.Include(fname, includedDoc) =>
            IncludedFileDetails(fname, rdoc.resolver(includedDoc), docPackageName(includedDoc))
        }
    // this contains a map of the included files to their
    // namespaces. This is used to qualify references to the included
    // type, which will appear as `scoped` identifiers in the Scrooge
    // AST (e.g. in thrift it looks like `shared.Atom`). We will look
    // up that scope in this map and affix the full package name to
    // the identifier in the generated scala code.
    val namespaceMap: IncludedTypesMap = includedDocs.collect {
        case IncludedFileDetails(fname, _, namespace) =>
          val key = (new File(fname).getName).replaceAll("\\.[^.]+$", "")
          key -> namespace.map(_.generate)
      }.toMap
    val packages = GeneratedPackage(generateDefinitions(rdoc, recurse, fname, namespaceMap), docPackageName(rdoc.document)) +:
      (if(recurse) {
        includedDocs.flatMap {
          case IncludedFileDetails(includedFname, rdoc, _) =>
            generatePackage(rdoc, new File(includedFname), recurse)
        }
      } else Nil)
    // merge the packages so that each one appears only once (and
    // thereby removing duplicate entries, which would otherwise
    // render the file uncompilable)
    packages.foldLeft(Map.empty: Map[Identifier, GeneratedPackage]) { (acc, pkg) =>
      val key = pkg.name.getOrElse(Identifier("_root_"))
      val updatedValue = acc.get(key).map(_ ++ pkg).getOrElse(pkg)
      acc.updated(key, updatedValue)
    }.values.toSeq
  }
}
