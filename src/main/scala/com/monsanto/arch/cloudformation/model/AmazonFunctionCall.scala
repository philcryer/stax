package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

import scala.language.implicitConversions

/**
 * Created by Ryan Richt on 2/15/15
 */

abstract class AmazonFunctionCall(val funName: String){type T ; val arguments: T}
object AmazonFunctionCall extends DefaultJsonProtocol {

  def lazyWriter[T](format: => JsonWriter[T]) = new JsonWriter[T] {
    lazy val delegate = format
    def write(x: T) = delegate.write(x)
  }

  //TODO: one day if we carry around T in Token[T], and dont erase as in AFC[_], this could be more generic
  implicit val format: JsonWriter[AmazonFunctionCall] = lazyWriter(new JsonWriter[AmazonFunctionCall] with DefaultJsonProtocol {
     def write(obj: AmazonFunctionCall) = {

      val value = obj match{
        case r:   Ref             => implicitly[JsonWriter[Ref#T]             ].write(r.arguments)
        case ga:  `Fn::GetAtt`    => implicitly[JsonWriter[`Fn::GetAtt`#T]    ].write(ga.arguments)
        case j:   `Fn::Join`      => implicitly[JsonWriter[`Fn::Join`#T]      ].write(j.arguments)
        case fim: `Fn::FindInMap` => implicitly[JsonWriter[`Fn::FindInMap`#T] ].write(fim.arguments)
        case b64: `Fn::Base64`    => implicitly[JsonWriter[`Fn::Base64`#T]    ].write(b64.arguments)
      }

      JsObject(
        obj.funName -> value
      )
    }
  })
}
case class Ref(variable: String          )
  extends AmazonFunctionCall("Ref"){type T = String ; val arguments = variable}

case class `Fn::GetAtt`(args: Seq[String])
  extends AmazonFunctionCall("Fn::GetAtt"){type T = Seq[String] ; val arguments = args}

case class `Fn::Join`(joinChar: String, toJoin: Seq[Token])
  extends AmazonFunctionCall("Fn::Join"){type T = (String, Seq[Token]) ; val arguments = (joinChar, toJoin)}

case class `Fn::FindInMap`(mapName: Token, outerKey: Token, innerKey: Token)
  extends AmazonFunctionCall("Fn::FindInMap"){type T = (Token, Token, Token); val arguments = (mapName, outerKey, innerKey)}

case class `Fn::Base64`(toEncode: Token)
  extends AmazonFunctionCall("Fn::Base64"){type T = Token ; val arguments = toEncode}


object `Fn::Base64` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`Fn::Base64`] = new JsonFormat[`Fn::Base64`] {

    def write(obj: `Fn::Base64`) = implicitly[JsonWriter[AmazonFunctionCall]].write(obj)

    //TODO
    def read(json: JsValue) = ???
  }
}

// TODO: Parameterize Token with its return type and enforce type safety of things like
// TODO: actual strings vs. CIDR blocks vs ARNs...
sealed trait Token
object Token extends DefaultJsonProtocol {
  implicit def fromStrings(s: String): StringToken = StringToken(s)
  implicit def fromFunctions(f: AmazonFunctionCall): FunctionCallToken = FunctionCallToken(f)

  // lazyFormat b/c Token and AmazonFunctionCall are mutually recursive
  implicit val format: JsonFormat[Token] = lazyFormat(new JsonFormat[Token] {
    def write(obj: Token) = {
      obj match {
        case s: StringToken => s.value.toJson
        case f: FunctionCallToken => implicitly[JsonWriter[AmazonFunctionCall]].write(f.call)
      }
    }

    // TODO: BLERG, for now, to make Tuple formats work
    def read(json: JsValue) = ???
  })
}
case class StringToken(value: String) extends Token
case class FunctionCallToken(call: AmazonFunctionCall) extends Token