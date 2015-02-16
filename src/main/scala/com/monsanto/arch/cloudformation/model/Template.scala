package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

/**
 * Created by Ryan Richt on 2/15/15
 */
case class Template(
                    AWSTemplateFormatVersion: String,
                    Description: String,
                    Parameters:  Option[Seq[Parameter]],
                    Mappings:    Option[Seq[Mapping]],
                    Resources:   Option[Seq[Resource]],
                    Outputs:     Option[Seq[Output]]
                   )
object Template extends DefaultJsonProtocol {

  // b/c we really dont need to implement READING yet, and its a bit trickier
  implicit def optionWriter[T : JsonWriter]: JsonWriter[Option[T]] = new JsonWriter[Option[T]] {
    def write(option: Option[T]) = option match {
      case Some(x) => x.toJson
      case None => JsNull
    }
  }

  implicit val format: JsonWriter[Template] = new JsonWriter[Template]{
    def write(p: Template) = {
      val fields = new collection.mutable.ListBuffer[(String, JsValue)]
      fields ++= productElement2Field[String]("AWSTemplateFormatVersion", p, 0)
      fields ++= productElement2Field[String]("Description", p, 1)
      fields ++= productElement2Field[Option[Seq[Parameter]]]("Parameters", p, 2)
      fields ++= productElement2Field[Option[Seq[Mapping]]]("Mappings", p, 3)
      fields ++= productElement2Field[Option[Seq[Resource]]]("Resources", p, 4)
      fields ++= productElement2Field[Option[Seq[Output]]]("Outputs", p, 5)
      JsObject(fields: _*)
    }
  }
}