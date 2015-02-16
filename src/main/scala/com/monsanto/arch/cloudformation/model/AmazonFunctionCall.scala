package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

import scala.language.implicitConversions

/**
 * Created by Ryan Richt on 2/15/15
 */

abstract class AmazonFunctionCall[T](val funName: String, val arguments: T)
object AmazonFunctionCall extends DefaultJsonProtocol {

  def lazyWriter[T](format: => JsonWriter[T]) = new JsonWriter[T] {
    lazy val delegate = format
    def write(x: T) = delegate.write(x)
  }

  //TODO: one day if we carry around T in Token[T], and dont erase as in AFC[_], this could be more generic
  implicit val format: JsonWriter[AmazonFunctionCall[_]] = lazyWriter(new JsonWriter[AmazonFunctionCall[_]] with DefaultJsonProtocol {
     def write(obj: AmazonFunctionCall[_]) = {

      val value = obj match{
        case r: Ref => implicitly[JsonWriter[String]].write(obj.asInstanceOf[AmazonFunctionCall[String]].arguments)
        case ga: `Fn::GetAtt` => implicitly[JsonWriter[Seq[String]]].write(obj.asInstanceOf[AmazonFunctionCall[Seq[String]]].arguments)
        case j: `Fn::Join` => implicitly[JsonWriter[(String, Seq[Token])]].write(obj.asInstanceOf[AmazonFunctionCall[(String, Seq[Token])]].arguments)
        case fim: `Fn::FindInMap` => implicitly[JsonWriter[(Token, Token, Token)]].write(obj.asInstanceOf[AmazonFunctionCall[(Token, Token, Token)]].arguments)
        case b64: `Fn::Base64` => implicitly[JsonWriter[Token]].write(obj.asInstanceOf[AmazonFunctionCall[Token]].arguments)
      }

      JsObject(
        obj.funName -> value
      )
    }
  })
}
case class Ref(variable: String          ) extends AmazonFunctionCall[String     ]("Ref"       , variable)
case class `Fn::GetAtt`(args: Seq[String]) extends AmazonFunctionCall[Seq[String]]("Fn::GetAtt", args    )
case class `Fn::Join`(joinChar: String, toJoin: Seq[Token]) extends AmazonFunctionCall("Fn::Join", (joinChar, toJoin))
case class `Fn::FindInMap`(mapName: Token, outerKey: Token, innerKey: Token) extends AmazonFunctionCall("Fn::FindInMap", (mapName, outerKey, innerKey))
case class `Fn::Base64`(toEncode: Token) extends AmazonFunctionCall("Fn::Base64", toEncode)
object `Fn::Base64` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`Fn::Base64`] = new JsonFormat[`Fn::Base64`] {

    def write(obj: `Fn::Base64`) = implicitly[JsonWriter[AmazonFunctionCall[_]]].write(obj)

    //TODO
    def read(json: JsValue) = ???
  }
}

// TODO: Parameterize Token with its return type and enforce type safety of things like
// TODO: actual strings vs. CIDR blocks vs ARNs...
sealed trait Token
object Token extends DefaultJsonProtocol {
  implicit def fromStrings(s: String): StringToken = StringToken(s)
  implicit def fromFunctions[T](f: AmazonFunctionCall[T]): FunctionCallToken[T] = FunctionCallToken(f)

  // lazyFormat b/c Token and AmazonFunctionCall are mutually recursive
  implicit val format: JsonFormat[Token] = lazyFormat(new JsonFormat[Token] {
    def write(obj: Token) = {
      obj match {
        case s: StringToken => s.value.toJson
        case f: FunctionCallToken[_] => implicitly[JsonWriter[AmazonFunctionCall[_]]].write(f.call)
      }
    }

    // TODO: BLERG, for now, to make Tuple formats work
    def read(json: JsValue) = ???
  })
}
case class StringToken(value: String) extends Token
case class FunctionCallToken[T](call: AmazonFunctionCall[T]) extends Token