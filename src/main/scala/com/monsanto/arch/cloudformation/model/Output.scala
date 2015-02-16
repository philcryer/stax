package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

/**
 * Created by Ryan Richt on 2/15/15
 */

case class Output(name: String, Description: String, Value: AmazonFunctionCall)
object Output extends DefaultJsonProtocol {

  implicit val seqFormat: JsonWriter[Seq[Output]] = new JsonWriter[Seq[Output]] {

    implicit val format: JsonWriter[Output] = new JsonWriter[Output] {
      def write(obj: Output) =
        JsObject(
          "Description" -> JsString(obj.Description),
          "Value"       -> implicitly[JsonWriter[AmazonFunctionCall]].write(obj.Value)
        )
    }

    def write(objs: Seq[Output]) = JsObject(objs.map(o => o.name -> format.write(o)).toMap)
  }
}