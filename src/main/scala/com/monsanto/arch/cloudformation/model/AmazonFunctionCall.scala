package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

import scala.language.implicitConversions

/**
 * Created by Ryan Richt on 2/15/15
 */

abstract class AmazonFunctionCall[R](val funName: String){type T ; val arguments: T}
object AmazonFunctionCall extends DefaultJsonProtocol {

  def lazyWriter[T](format: => JsonWriter[T]) = new JsonWriter[T] {
    lazy val delegate = format
    def write(x: T) = delegate.write(x)
  }

  //TODO: one day if we carry around T in Token[T], and dont erase as in AFC[_], this could be more generic
  implicit val format: JsonWriter[AmazonFunctionCall[_]] = lazyWriter(new JsonWriter[AmazonFunctionCall[_]] with DefaultJsonProtocol {
     def write(obj: AmazonFunctionCall[_]) = {

      val value = obj match{
        case r:   Ref             => implicitly[JsonWriter[Ref#T]             ].write(r.arguments)
        case r:   ParameterRef[_] => implicitly[JsonWriter[ParameterRef[_]#T] ].write(r.arguments)
        case r:   ResourceRef[_]  => implicitly[JsonWriter[ResourceRef[_]#T]  ].write(r.arguments)
        case ga:  `Fn::GetAtt`    => implicitly[JsonWriter[`Fn::GetAtt`#T]    ].write(ga.arguments)
        case j:   `Fn::Join`      => implicitly[JsonWriter[`Fn::Join`#T]      ].write(j.arguments)
        case fim: `Fn::FindInMap` => implicitly[JsonWriter[`Fn::FindInMap`#T] ].write(fim.arguments)
        case b64: `Fn::Base64`    => implicitly[JsonWriter[`Fn::Base64`#T]    ].write(b64.arguments)
        case eq: `Fn::Equals`    => implicitly[JsonWriter[`Fn::Equals`#T]    ].write(eq.arguments)
      }

      JsObject(
        obj.funName -> value
      )
    }
  })
}

@deprecated("use ParameterRef or ResourceRef instead", "Feb 16 2015")
case class Ref (variable: String)
  extends AmazonFunctionCall[String]("Ref"){type T = String ; val arguments = variable}

case class ParameterRef[R](p: Parameter{type Rep = R})
  extends AmazonFunctionCall[R]("Ref"){type T = String ; val arguments = p.name}

case class ResourceRef[R <: Resource](r: R)
  extends AmazonFunctionCall[R]("Ref"){type T = String ; val arguments = r.name}

case class `Fn::GetAtt`(args: Seq[String])
  extends AmazonFunctionCall[String]("Fn::GetAtt"){type T = Seq[String] ; val arguments = args}

case class `Fn::Join`(joinChar: String, toJoin: Seq[Token[String]])
  extends AmazonFunctionCall[String]("Fn::Join"){type T = (String, Seq[Token[String]]) ; val arguments = (joinChar, toJoin)}

case class `Fn::FindInMap`(mapName: Token[String], outerKey: Token[String], innerKey: Token[String])
  extends AmazonFunctionCall[String]("Fn::FindInMap"){type T = (Token[String], Token[String], Token[String]); val arguments = (mapName, outerKey, innerKey)}

case class `Fn::Base64`(toEncode: Token[String])
  extends AmazonFunctionCall[String]("Fn::Base64"){type T = Token[String] ; val arguments = toEncode}

//TODO: NOT TESTED YET
case class `Fn::Equals`(a: Token[String], b: Token[String])
  extends AmazonFunctionCall[String]("Fn::Equals"){type T = (Token[String], Token[String]) ; val arguments = (a, b)}


object `Fn::Base64` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`Fn::Base64`] = new JsonFormat[`Fn::Base64`] {

    def write(obj: `Fn::Base64`) = implicitly[JsonWriter[AmazonFunctionCall[_]]].write(obj)

    //TODO
    def read(json: JsValue) = ???
  }
}

// TODO: Parameterize Token with its return type and enforce type safety of things like
// TODO: actual strings vs. CIDR blocks vs ARNs...
sealed trait Token[R]
object Token extends DefaultJsonProtocol {
  implicit def fromAny[R: JsonFormat](r: R): AnyToken[R] = AnyToken(r)
  implicit def fromStrings(s: String): StringToken = StringToken(s)
  implicit def fromFunctions[R](f: AmazonFunctionCall[R]): FunctionCallToken[R] = FunctionCallToken[R](f)

  // lazyFormat b/c Token and AmazonFunctionCall are mutually recursive
  implicit def format[R : JsonFormat]: JsonFormat[Token[R]] = lazyFormat(new JsonFormat[Token[R]] {
    def write(obj: Token[R]) = {
      obj match {
        case a: AnyToken[R] => a.value.toJson
        case s: StringToken => s.value.toJson
          // its OK to erase the return type of AmazonFunctionCalls b/c they are only used at compile time for checking
          // not for de/serialization logic or JSON representation
        case f: FunctionCallToken[_] => implicitly[JsonWriter[AmazonFunctionCall[_]]].write(f.call)
      }
    }

    // TODO: BLERG, for now, to make Tuple formats work
    def read(json: JsValue) = ???
  })
}
case class AnyToken[R : JsonFormat](value: R) extends Token[R]
case class StringToken(value: String) extends Token[String]
case class FunctionCallToken[R](call: AmazonFunctionCall[R]) extends Token[R]