package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

/**
 * Created by Ryan Richt on 2/15/15
 */

case class Output[T](name: String, Description: String, Value: AmazonFunctionCall[T])
object Output extends DefaultJsonProtocol {

  implicit val seqFormat: JsonWriter[Seq[Output[_]]] = new JsonWriter[Seq[Output[_]]] {

    implicit val format: JsonWriter[Output[_]] = new JsonWriter[Output[_]] {
      def write(obj: Output[_]) =
        JsObject(
          "Description" -> JsString(obj.Description),
          "Value" -> implicitly[JsonWriter[AmazonFunctionCall[_]]].write(obj.Value)
        )
    }

    def write(objs: Seq[Output[_]]) = JsObject(objs.map(o => o.name -> format.write(o)).toMap)
  }
}