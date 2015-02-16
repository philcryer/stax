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

case class StringParameter(
                            name:                  String,
                            Description:           String,
                            MinLength:             Option[String]         = None, // TODO Int reformatted to string
                            MaxLength:             Option[String]         = None, // TODO Int reformatted to string
                            AllowedPattern:        Option[String]      = None,
                            ConstraintDescription: Option[String]      = None,
                            Default:               Option[String]      = None,
                            AllowedValues:         Option[Seq[String]] = None
                            ) extends Parameter("String")
object StringParameter extends DefaultJsonProtocol {

  implicit val format: JsonFormat[StringParameter] = jsonFormat8(StringParameter.apply)

  def apply(
             name:                  String,
             Description:           String,
             MinLength:             String, // TODO Int reformatted to string
             MaxLength:             String, // TODO Int reformatted to string
             AllowedPattern:        String,
             ConstraintDescription: String,
             Default:               String
             ): StringParameter = StringParameter(
                                                   name,
                                                   Description,
                                                   Some(MinLength),
                                                   Some(MaxLength),
                                                   Some(AllowedPattern),
                                                   Some(ConstraintDescription),
                                                   Some(Default),
                                                   None
                                                 )
}

case class NumberParameter(
                            name:                  String,
                            Description:           String,
                            MinValue:              Option[String]         = None, // TODO Int reformatted to string
                            MaxValue:              Option[String]         = None, // TODO Int reformatted to string
                            ConstraintDescription: Option[String]      = None,
                            Default:               Option[String]      = None, // TODO Int reformatted to string
                            AllowedValues:         Option[Seq[String]] = None
                            ) extends Parameter("Number")
object NumberParameter extends DefaultJsonProtocol {

  implicit val format: JsonFormat[NumberParameter] = jsonFormat7(NumberParameter.apply)

  def apply(
             name:                  String,
             Description:           String,
             MinValue:              String, // TODO Int reformatted to string
             MaxValue:              String, // TODO Int reformatted to string
             ConstraintDescription: String,
             Default:               String // TODO Int reformatted to string
             ): NumberParameter = NumberParameter(
                                                   name,
                                                   Description,
                                                   Some(MinValue),
                                                   Some(MaxValue),
                                                   Some(ConstraintDescription),
                                                   Some(Default),
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