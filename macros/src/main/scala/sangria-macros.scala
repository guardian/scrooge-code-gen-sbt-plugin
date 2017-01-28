package com.gu.sangriascrooge

import scala.reflect.macros._

import com.twitter.scrooge.{ ThriftStruct, ThriftEnum, ThriftUnion }
import sangria.macros.derive.GraphQLOutputTypeLookup
import sangria.validation.IntCoercionViolation
import sangria.schema._

import scala.language.experimental.macros

trait SangriaMacros[Ctx] {
  implicit val shortType = ScalarType[Short](
      name = "Short",
    coerceUserInput = {
      case s: Short => Right(s)
      case i: Int if i.isValidShort => Right(i.toShort)
      case x => Left(IntCoercionViolation)
    },
    coerceOutput = valueOutput,
    coerceInput = {
      case sangria.ast.IntValue(i, _, _) if i.isValidShort => Right(i.toShort)
      case _ => Left(IntCoercionViolation)
    }
  )

  implicit def deriveOutputTypeLookupThriftStruct[T <: ThriftStruct]:
      GraphQLOutputTypeLookup[T] =
    macro MacroImpl.deriveOutputTypeLookupThriftStruct[Ctx, T]

  implicit def deriveOutputTypeLookupThriftUnion[T <: ThriftUnion with ThriftStruct]:
      GraphQLOutputTypeLookup[T] =
    macro MacroImpl.deriveOutputTypeLookupThriftUnion[Ctx, T]

  implicit def deriveThriftUnion[T <: ThriftUnion with ThriftStruct]: UnionType[Ctx] =
    macro MacroImpl.deriveThriftUnion[Ctx, T]

  implicit def deriveThriftStruct[T <: ThriftStruct]: ObjectType[Ctx, T] =
    macro MacroImpl.deriveThriftStruct[Ctx, T]
  // def deriveThriftEnum[T <: ThriftEnum]: EnumType[T] = macro MacroImpl.deriveThriftEnum[T]
}

// whitebox means: if we can't apply this macro in the implicit
// definition, then just ignore it, rather than throw an error
// message:
//
//   "When an application of a blackbox macro is used as an implicit
//   candidate, no expansion is performed until the macro is selected
//   as the result of the implicit search. This makes it impossible to
//   dynamically calculate availability of implicit macros."
//
// [ http://docs.scala-lang.org/overviews/macros/blackbox-whitebox ]
//
//   "There is a noticeable distinction between blackbox and whitebox
//   materializers. An error in an expansion of a blackbox implicit
//   macro (e.g. an explicit c.abort call or an expansion typecheck
//   error) will produce a compilation error. An error in an expansion
//   of a whitebox implicit macro will just remove the macro from the
//   list of implicit candidates in the current implicit search,
//   without ever reporting an actual error to the user."
//
// [ http://docs.scala-lang.org/overviews/macros/implicits.html#blackbox-vs-whitebox ]
//
// so the effect of this being a blackbox macro, were it to be one, is
// that it gets selected as a candidate for all types and when this
// fails if it type in question is not, in fact `T <: ThriftStruct`
// the implicit resolution fails.
//
// ... Or something like that. Macros are mysterious.

// class MacroImpl(val c: whitebox.Context) {
//   import c.universe._

class MacroImpl(val c: whitebox.Context) {
  import c.universe._

  def deriveThriftStruct[Ctx : c.WeakTypeTag, T: c.WeakTypeTag] = {
    val ctxType = weakTypeOf[Ctx]
    val tType   = weakTypeOf[T]

    val typeName = tType.typeSymbol.name.toTypeName

    val applyMethod = tType.companion.member(TermName("apply")).asMethod

    val fields = applyMethod.paramLists.head map { param =>
      val paramType = param.info
      val paramName = param.name.toTermName
      val fieldName = paramName.toString
      q"""sangria.schema.Field($fieldName,
            _root_.scala.Predef.implicitly[_root_.shapeless.Lazy[_root_.sangria.macros.derive.GraphQLOutputTypeLookup[$paramType]]].value.graphqlType,
            resolve = (_:_root_.sangria.schema.Context[$ctxType, $tType]).value.${paramName})"""
    }

    q"""sangria.schema.ObjectType[$ctxType, $tType](name = ${typeName.decodedName.toString}, fields = $fields)"""
  }

  def deriveThriftUnion[Ctx : c.WeakTypeTag, T: c.WeakTypeTag] = {
    val ctxType = weakTypeOf[Ctx]
    val tType   = weakTypeOf[T]
    val typeName = tType.typeSymbol.name.toTypeName

    val companion = tType.companion
    val unionTypes = companion
      .members
      .filter(m => m.isClass && m.asType.toType <:< tType && !m.name.toString.startsWith("UnknownUnionField"))
      .map { cl =>
        val clCompanion = cl.asType.toTypeIn(companion).companion
        val param = clCompanion.member(TermName("apply"))
          .asMethod.paramLists.head.head
        val dealiased = param.typeSignature.dealias
        if(!(dealiased <:< typeOf[ThriftStruct]))
          c.error(c.enclosingPosition, s"Union subtype $dealiased is not a thrift struct, can't create ObjectType from it")
        q"""_root_.scala.Predef.implicitly[
          _root_.shapeless.Lazy[_root_.sangria.schema.ObjectType[$ctxType, $dealiased]]
        ].value"""
        //q"""_root_.scala.Predef.implicitly[_root_.shapeless.Lazy[
          //_root_.sangria.macros.derive.GraphQLOutputTypeLookup[${dealiased}]]
        //].value.graphqlType"""
      }
          //val paramName = param.name.toTermName

      //if(dealiased.toType <:< typeOf[ThriftStruct])
        //q"""${deriveThriftStruct[Ctx, T]}"""
      //else
        //q"""_root_.scala.Predef.implicitly[_root_.shapeless.Lazy[_root_.sangria.macros.derive.GraphQLOutputTypeLookup[${dealiased}]]].value.graphqlType"""
    //}
    q"""{
      sangria.schema.UnionType[$ctxType](name = ${typeName.decodedName.toString}, types = ${unionTypes.toList})
    }"""
  }

  def deriveOutputTypeLookupThriftStruct[Ctx: c.WeakTypeTag, T: c.WeakTypeTag] = {
    val tType = weakTypeOf[T]
    q"""new sangria.macros.derive.GraphQLOutputTypeLookup[$tType] {
          def graphqlType = ${deriveThriftStruct[Ctx, T]}
        }"""
  }

  def deriveOutputTypeLookupThriftUnion[Ctx: c.WeakTypeTag, T: c.WeakTypeTag] = {
    val tType = weakTypeOf[T]
    q"""new sangria.macros.derive.GraphQLOutputTypeLookup[$tType] {
          def graphqlType = ${deriveThriftUnion[Ctx, T]}
        }"""
  }

  //def deriveThriftEnum[T: c.WeakTypeTag] = {
  //}
}
