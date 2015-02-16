package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

/**
 * Created by Ryan Richt on 2/15/15
 */

sealed trait Mapping{type T; val name: String; val map: Map[String, Map[String, T]]}
object Mapping extends DefaultJsonProtocol {

  implicit object seqFormat extends JsonWriter[Seq[Mapping]] {

    implicit object format extends JsonWriter[Mapping] with DefaultJsonProtocol {
      def write(obj: Mapping) = {

        val raw = obj match {
          case s: StringMapping => s.map.toJson
          case n: SeqStringMapping => n.map.toJson
        }

        JsObject(raw.asJsObject.fields - "name")
      }
    }

    def write(objs: Seq[Mapping]) = JsObject(objs.map(o => o.name -> o.toJson).toMap)
  }
}

case class StringMapping(   name: String, map: Map[String, Map[String,     String]] ) extends Mapping{type T = String}
case class SeqStringMapping(name: String, map: Map[String, Map[String, Seq[String]]]) extends Mapping{type T = Seq[String]}