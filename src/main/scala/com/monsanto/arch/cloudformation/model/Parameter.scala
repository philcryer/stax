package com.monsanto.arch.cloudformation.model

import spray.json._

/**
 * Created by Ryan Richt on 2/15/15
 */

sealed abstract class Parameter(val Type: String) {
  def name:        String
  def Description: String
}
object Parameter extends DefaultJsonProtocol {
  implicit object seqFormat extends JsonWriter[Seq[Parameter]]{

    implicit object format extends JsonWriter[Parameter]{
      def write(obj: Parameter) = {

        val raw = obj match {
          case s: StringParameter => s.toJson
          case n: NumberParameter => n.toJson
          case k: `AWS::EC2::KeyPair::KeyName_Parameter` => k.toJson
        }

        JsObject( raw.asJsObject.fields - "name" + ("Type" -> JsString(obj.Type)) )
      }
    }

    def write(objs: Seq[Parameter]) = JsObject( objs.map( o => o.name -> o.toJson ).toMap )
  }
}

case class StringBackedInt(value: Int)
object StringBackedInt extends DefaultJsonProtocol {
  implicit val format: JsonFormat[StringBackedInt] = new JsonFormat[StringBackedInt]{
    def write(obj: StringBackedInt) = JsString(obj.value.toString)
    def read(json: JsValue) = StringBackedInt( json.convertTo[String].toInt )
  }
}

case class StringParameter private (
                            name:                  String,
                            Description:           String,
                            MinLength:             Option[StringBackedInt],
                            MaxLength:             Option[StringBackedInt],
                            AllowedPattern:        Option[String],
                            ConstraintDescription: Option[String],
                            Default:               Option[String],
                            AllowedValues:         Option[Seq[String]]
                            ) extends Parameter("String")
object StringParameter extends DefaultJsonProtocol {

  // all these types to pick out the correct "apply" from the two choices
  implicit val format: JsonFormat[StringParameter] =
    jsonFormat8[String, String, Option[StringBackedInt], Option[StringBackedInt], Option[String],
      Option[String], Option[String], Option[Seq[String]], StringParameter](StringParameter.apply)

  def apply(
             name:                  String,
             Description:           String,
             MinLength:             Int,
             MaxLength:             Int,
             Default:               String,
             ConstraintDescription: Option[String]      = None,
             AllowedPattern:        Option[String]      = None,
             AllowedValues:         Option[Seq[String]] = None
             ): StringParameter = StringParameter(
                                                   name,
                                                   Description,
                                                   Some(StringBackedInt(MinLength)),
                                                   Some(StringBackedInt(MaxLength)),
                                                   AllowedPattern,
                                                   ConstraintDescription,
                                                   Some(Default),
                                                   AllowedValues
                                                 )

    def apply(
             name:                  String,
             Description:           String,
             MinLength:             Option[Int]         ,
             MaxLength:             Option[Int]         ,
             Default:               String,
             ConstraintDescription: Option[String],
             AllowedPattern:        Option[String],
             AllowedValues:         Option[Seq[String]]
             ): StringParameter = StringParameter(
                                                   name,
                                                   Description,
                                                   MinLength.map(StringBackedInt.apply),
                                                   MaxLength.map(StringBackedInt.apply),
                                                   AllowedPattern,
                                                   ConstraintDescription,
                                                   Some(Default),
                                                   AllowedValues
                                                 )

    def apply(
             name:                  String,
             Description:           String,
             Default:               String,
             ConstraintDescription: String,
             AllowedValues:         Seq[String]
             ): StringParameter = StringParameter(
                                                   name,
                                                   Description,
                                                   None,
                                                   None,
                                                   None,
                                                   Some(ConstraintDescription),
                                                   Some(Default),
                                                   Some(AllowedValues)
                                                 )

    def apply(name: String, Description: String): StringParameter = StringParameter(name, Description, None, None, None, None, None, None)
    def apply(name: String, Description: String, Default: String): StringParameter = StringParameter(name, Description, None, None, None, None, Some(Default), None)
    def apply(name: String, Description: String, AllowedValues: Seq[String], Default: String): StringParameter = StringParameter(name, Description, None, None, None, None, Some(Default), Some(AllowedValues))
}

case class NumberParameter private (
                            name:                  String,
                            Description:           String,
                            MinValue:              Option[StringBackedInt],
                            MaxValue:              Option[StringBackedInt],
                            ConstraintDescription: Option[String],
                            Default:               Option[StringBackedInt],
                            AllowedValues:         Option[Seq[StringBackedInt]]
                            ) extends Parameter("Number")
object NumberParameter extends DefaultJsonProtocol {

  implicit val format: JsonFormat[NumberParameter] = jsonFormat7(NumberParameter.apply)

  def apply(
             name:                  String,
             Description:           String,
             MinValue:              Int,
             MaxValue:              Int,
             Default:               Int,
             ConstraintDescription: Option[String] = None
    ): NumberParameter = NumberParameter(
                                                   name,
                                                   Description,
                                                   Some(StringBackedInt(MinValue)),
                                                   Some(StringBackedInt(MaxValue)),
                                                   ConstraintDescription,
                                                   Some(StringBackedInt(Default)),
                                                   None
                                                 )
}

case class `AWS::EC2::KeyPair::KeyName_Parameter`(
                                                  name:                  String,
                                                  Description:           String,
                                                  ConstraintDescription: Option[String] = None
                                                  ) extends Parameter("AWS::EC2::KeyPair::KeyName")
object `AWS::EC2::KeyPair::KeyName_Parameter` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::KeyPair::KeyName_Parameter`] = jsonFormat3(`AWS::EC2::KeyPair::KeyName_Parameter`.apply)
}