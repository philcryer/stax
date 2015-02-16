package com.monsanto.arch.cloudformation.model

import spray.json._
import DefaultJsonProtocol._

import scala.language.implicitConversions

/**
 * Created by Ryan Richt on 2/15/15
 */

// serializes to Type and Properties
sealed abstract class Resource(val Type: String){val name: String}
object Resource extends DefaultJsonProtocol {
  implicit object seqFormat extends JsonWriter[Seq[Resource]]{

    implicit object format extends JsonWriter[Resource]{
      def write(obj: Resource) = {

        val raw = obj match {
          case r: `AWS::AutoScaling::AutoScalingGroup`      => r.toJson
          case r: `AWS::AutoScaling::LaunchConfiguration`   => r.toJson
          case r: `AWS::AutoScaling::ScalingPolicy`         => r.toJson
          case r: `AWS::EC2::EIP`                           => r.toJson
          case r: `AWS::EC2::Instance`                      => r.toJson
          case r: `AWS::EC2::InternetGateway`               => r.toJson
          case r: `AWS::EC2::KeyPair::KeyName`              => r.toJson
          case r: `AWS::EC2::Route`                         => r.toJson
          case r: `AWS::EC2::RouteTable`                    => r.toJson
          case r: `AWS::EC2::SecurityGroup`                 => r.toJson
          case r: `AWS::EC2::SecurityGroupEgress`           => r.toJson
          case r: `AWS::EC2::SecurityGroupIngress`          => r.toJson
          case r: `AWS::EC2::Subnet`                        => r.toJson
          case r: `AWS::EC2::SubnetRouteTableAssociation`   => r.toJson
          case r: `AWS::EC2::VPC`                           => r.toJson
          case r: `AWS::EC2::VPCGatewayAttachment`          => r.toJson
          case r: `AWS::ElasticLoadBalancing::LoadBalancer` => r.toJson
          case r: `AWS::IAM::InstanceProfile`               => r.toJson
          case r: `AWS::IAM::Role`                          => r.toJson
        }

        val mainFields = JsObject(raw.asJsObject.fields - "name")
        mainFields.fields.get("Metadata") match {
          case Some(meta) => JsObject( "Type" -> JsString(obj.Type), "Metadata" -> meta, "Properties" -> JsObject(mainFields.fields - "Metadata") )
          case None       => JsObject( "Type" -> JsString(obj.Type),                     "Properties" -> mainFields )
        }
      }
    }

    def write(objs: Seq[Resource]) = JsObject( objs.map( o => o.name -> o.toJson ).toMap )
  }
}

case class `AWS::AutoScaling::AutoScalingGroup`(
                                                 name: String,
                                                 AvailabilityZones: Seq[String],
                                                 LaunchConfigurationName: Token[String],
                                                 MinSize: String,
                                                 MaxSize: String,
                                                 DesiredCapacity: Token[String],
                                                 HealthCheckType: String,
                                                 VPCZoneIdentifier: Seq[Token[String]],
                                                 Tags: Seq[AmazonTag],
                                                 LoadBalancerNames: Option[Seq[Token[String]]]
                                                 ) extends Resource("AWS::AutoScaling::AutoScalingGroup")
object `AWS::AutoScaling::AutoScalingGroup` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::AutoScaling::AutoScalingGroup`] = jsonFormat10(`AWS::AutoScaling::AutoScalingGroup`.apply)
}

case class `AWS::AutoScaling::LaunchConfiguration`(
                                                    name: String,
                                                    ImageId: Token[String],
                                                    InstanceType: Token[String],
                                                    KeyName: Token[String],
                                                    SecurityGroups: Seq[Token[String]],
                                                    UserData: `Fn::Base64`
                                                    ) extends Resource("AWS::AutoScaling::LaunchConfiguration")
object `AWS::AutoScaling::LaunchConfiguration` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::AutoScaling::LaunchConfiguration`] = jsonFormat6(`AWS::AutoScaling::LaunchConfiguration`.apply)
}

case class `AWS::AutoScaling::ScalingPolicy`(
                                              name: String,
                                              AdjustmentType: String,
                                              AutoScalingGroupName: Token[String],
                                              Cooldown: Token[String],
                                              ScalingAdjustment: String
                                              ) extends Resource("AWS::AutoScaling::ScalingPolicy")
object `AWS::AutoScaling::ScalingPolicy` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::AutoScaling::ScalingPolicy`] = jsonFormat5(`AWS::AutoScaling::ScalingPolicy`.apply)
}

case class `AWS::EC2::EIP`(name: String, Domain: String, InstanceId: Token[String]) extends Resource("AWS::EC2::EIP")
object `AWS::EC2::EIP` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::EIP`] = jsonFormat3(`AWS::EC2::EIP`.apply)
}

case class `AWS::EC2::Instance`(
                                 name: String,
                                 InstanceType: Token[String],
                                 KeyName: Token[String],
                                 SubnetId: Token[String],
                                 ImageId: Token[String],
                                 SecurityGroupIds: Seq[Token[String]],
                                 Tags: Seq[AmazonTag],
                                 Metadata: Option[Map[String, String]] = None,
                                 IamInstanceProfile: Option[Token[String]] = None,
                                 SourceDestCheck: Option[String] = None,
                                 UserData: Option[`Fn::Base64`] = None
                                 ) extends Resource("AWS::EC2::Instance")
object `AWS::EC2::Instance` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::Instance`] = jsonFormat11(`AWS::EC2::Instance`.apply)
}

case class `AWS::EC2::InternetGateway`(name: String, Tags: Seq[AmazonTag]) extends Resource("AWS::EC2::InternetGateway")
object `AWS::EC2::InternetGateway` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::InternetGateway`] = jsonFormat2(`AWS::EC2::InternetGateway`.apply)
}

case class `AWS::EC2::KeyPair::KeyName`(name: String) extends Resource("AWS::EC2::KeyPair::KeyName")
object `AWS::EC2::KeyPair::KeyName` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::KeyPair::KeyName`] = jsonFormat1(`AWS::EC2::KeyPair::KeyName`.apply)
}

case class `AWS::EC2::Route`(
                              name: String,
                              RouteTableId: Token[String],
                              DestinationCidrBlock: String,
                              GatewayId: Option[Token[String]] = None,
                              InstanceId: Option[Token[String]] = None
                              ) extends Resource("AWS::EC2::Route")
object `AWS::EC2::Route` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::Route`] = jsonFormat5(`AWS::EC2::Route`.apply)
}

case class `AWS::EC2::RouteTable`(name: String, VpcId: Token[`AWS::EC2::VPC`], Tags: Seq[AmazonTag]) extends Resource("AWS::EC2::RouteTable")
object `AWS::EC2::RouteTable` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::RouteTable`] = jsonFormat3(`AWS::EC2::RouteTable`.apply)
}

case class `AWS::EC2::SecurityGroup`(
                                      name: String,
                                      GroupDescription: String,
                                      VpcId: Token[`AWS::EC2::VPC`],
                                      SecurityGroupIngress: Option[Seq[IngressSpec]],
                                      SecurityGroupEgress: Option[Seq[EgressSpec]],
                                      Tags: Seq[AmazonTag]
                                      ) extends Resource("AWS::EC2::SecurityGroup")
object `AWS::EC2::SecurityGroup` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::SecurityGroup`] = jsonFormat6(`AWS::EC2::SecurityGroup`.apply)
}

sealed trait IngressSpec
object IngressSpec extends DefaultJsonProtocol {
  implicit val format: JsonFormat[IngressSpec] = new JsonFormat[IngressSpec] {
    def write(obj: IngressSpec) =
      obj match {
        case i: CidrIngressSpec => i.toJson
        case i: SGIngressSpec => i.toJson
      }
    //TODO
    def read(json: JsValue) = ???
  }
}
case class CidrIngressSpec(IpProtocol: String, CidrIp: Token[CidrIp], FromPort: String, ToPort: String) extends IngressSpec
object CidrIngressSpec extends DefaultJsonProtocol {
  implicit val format: JsonFormat[CidrIngressSpec] = jsonFormat4(CidrIngressSpec.apply)
}
case class SGIngressSpec(IpProtocol: String, SourceSecurityGroupId: Token[String], FromPort: String, ToPort: String) extends IngressSpec
object SGIngressSpec extends DefaultJsonProtocol {
  implicit val format: JsonFormat[SGIngressSpec] = jsonFormat4(SGIngressSpec.apply)
}

case class IPAddressSegment(value: Int){ require( value <= 255 && value >= 0 ) }
object IPAddressSegment {
  implicit def fromInt(i: Int): IPAddressSegment = IPAddressSegment(i)
}

case class IPMask(value: Int){ require( value <= 32 && value >= 0 ) }
object IPMask {
  implicit def fromInt(i: Int): IPMask = IPMask(i)
}

case class CidrIp(a: IPAddressSegment, b: IPAddressSegment, c: IPAddressSegment, d: IPAddressSegment, mask: IPMask)
object CidrIp extends DefaultJsonProtocol {
  implicit val format: JsonFormat[CidrIp] = new JsonFormat[CidrIp] {
    def write(obj: CidrIp) = JsString( Seq(obj.a, obj.b, obj.c, obj.d).map(_.value.toString).mkString(".") + "/" + obj.mask.value.toString )

    def read(json: JsValue) = {
      val parts = json.convertTo[String].split(Array('.','/')).map(_.toInt)

      CidrIp(parts(0), parts(1), parts(2), parts(3), parts(4))
    }
  }
}

sealed trait EgressSpec
object EgressSpec extends DefaultJsonProtocol {
  implicit val format: JsonFormat[EgressSpec] = new JsonFormat[EgressSpec] {
    def write(obj: EgressSpec) =
      obj match {
        case i: CidrEgressSpec => i.toJson
        case i: SGEgressSpec => i.toJson
      }
    //TODO
    def read(json: JsValue) = ???
  }
}
case class CidrEgressSpec(IpProtocol: String, CidrIp: Token[CidrIp], FromPort: String, ToPort: String) extends EgressSpec
object CidrEgressSpec extends DefaultJsonProtocol {
  implicit val format: JsonFormat[CidrEgressSpec] = jsonFormat4(CidrEgressSpec.apply)
}
case class SGEgressSpec(IpProtocol: String, DestinationSecurityGroupId: Token[String], FromPort: String, ToPort: String) extends EgressSpec
object SGEgressSpec extends DefaultJsonProtocol {
  implicit val format: JsonFormat[SGEgressSpec] = jsonFormat4(SGEgressSpec.apply)
}


case class `AWS::EC2::SecurityGroupEgress`(
                                            name: String,
                                            GroupId: Token[String],
                                            IpProtocol: String,
                                            DestinationSecurityGroupId:
                                            Token[String],
                                            FromPort: String,
                                            ToPort: String
                                            ) extends Resource("AWS::EC2::SecurityGroupEgress")
object `AWS::EC2::SecurityGroupEgress` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::SecurityGroupEgress`] = jsonFormat6(`AWS::EC2::SecurityGroupEgress`.apply)
}

case class `AWS::EC2::SecurityGroupIngress`(
                                             name: String,
                                             GroupId: Token[String],
                                             IpProtocol: String,
                                             SourceSecurityGroupId: Token[String],
                                             FromPort: String,
                                             ToPort: String
                                             ) extends Resource("AWS::EC2::SecurityGroupIngress")
object `AWS::EC2::SecurityGroupIngress` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::SecurityGroupIngress`] = jsonFormat6(`AWS::EC2::SecurityGroupIngress`.apply)
}

case class `AWS::EC2::Subnet`(
                               name: String,
                               VpcId: Token[`AWS::EC2::VPC`],
                               AvailabilityZone: String,
                               CidrBlock: Token[String],
                               Tags: Seq[AmazonTag]
                               ) extends Resource("AWS::EC2::Subnet")
object `AWS::EC2::Subnet` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::Subnet`] = jsonFormat5(`AWS::EC2::Subnet`.apply)
}

case class `AWS::EC2::SubnetRouteTableAssociation`(
                                                    name: String,
                                                    SubnetId: Token[String],
                                                    RouteTableId: Token[String]
                                                    ) extends Resource("AWS::EC2::SubnetRouteTableAssociation")
object `AWS::EC2::SubnetRouteTableAssociation` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::SubnetRouteTableAssociation`] = jsonFormat3(`AWS::EC2::SubnetRouteTableAssociation`.apply)
}

case class `AWS::EC2::VPC`(name: String, CidrBlock: Token[CidrIp], Tags: Seq[AmazonTag]) extends Resource("AWS::EC2::VPC")
object `AWS::EC2::VPC` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::VPC`] = jsonFormat3(`AWS::EC2::VPC`.apply)
}

case class AmazonTag(Key: String, Value: Token[String], PropagateAtLaunch: Option[Boolean] = None)
object AmazonTag extends DefaultJsonProtocol {
  implicit val format: JsonFormat[AmazonTag] = jsonFormat3(AmazonTag.apply)
}

case class `AWS::EC2::VPCGatewayAttachment`(name: String, VpcId: Token[`AWS::EC2::VPC`], InternetGatewayId: Token[String]) extends Resource("AWS::EC2::VPCGatewayAttachment")
object `AWS::EC2::VPCGatewayAttachment` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::EC2::VPCGatewayAttachment`] = jsonFormat3(`AWS::EC2::VPCGatewayAttachment`.apply)
}

case class `AWS::ElasticLoadBalancing::LoadBalancer`(
                                                      name: String,
                                                      CrossZone: Boolean,
                                                      SecurityGroups: Seq[Token[String]],
                                                      Subnets: Seq[Token[String]],
                                                      Listeners: Seq[Listener],
                                                      HealthCheck: HealthCheck,
                                                      Tags: Seq[AmazonTag]
                                                      ) extends Resource("AWS::ElasticLoadBalancing::LoadBalancer")
object `AWS::ElasticLoadBalancing::LoadBalancer` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::ElasticLoadBalancing::LoadBalancer`] = jsonFormat7(`AWS::ElasticLoadBalancing::LoadBalancer`.apply)
}

case class Listener(LoadBalancerPort: String, InstancePort: String, Protocol: String)
object Listener extends DefaultJsonProtocol {
  implicit val format: JsonFormat[Listener] = jsonFormat3(Listener.apply)
}
case class HealthCheck(Target: String, HealthyThreshold: String, UnhealthyThreshold: String, Interval: String, Timeout: String)
object HealthCheck extends DefaultJsonProtocol {
  implicit val format: JsonFormat[HealthCheck] = jsonFormat5(HealthCheck.apply)
}

case class `AWS::IAM::InstanceProfile`(name: String, Path: String, Roles: Seq[Token[String]]) extends Resource("AWS::IAM::InstanceProfile")
object `AWS::IAM::InstanceProfile` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::IAM::InstanceProfile`] = jsonFormat3(`AWS::IAM::InstanceProfile`.apply)
}


case class `AWS::IAM::Role`(
                             name: String,
                             AssumeRolePolicyDocument: PolicyDocument,
                             Policies: Seq[Policy],
                             Path: String
                             ) extends Resource("AWS::IAM::Role")
object `AWS::IAM::Role` extends DefaultJsonProtocol {
  implicit val format: JsonFormat[`AWS::IAM::Role`] = jsonFormat4(`AWS::IAM::Role`.apply)
}
case class PolicyStatement(
                            Effect: String,
                            Principal: Option[PolicyPrincipal] = None,
                            Action: Seq[String],
                            Resource: Option[Token[String]] = None
                            )
object PolicyStatement extends DefaultJsonProtocol {
  implicit val format: JsonFormat[PolicyStatement] = jsonFormat4(PolicyStatement.apply)
}

// TODO: Make this not a string

sealed trait PolicyPrincipal
object PolicyPrincipal extends DefaultJsonProtocol {
  implicit val format: JsonFormat[PolicyPrincipal] = new JsonFormat[PolicyPrincipal] {
    def write(obj: PolicyPrincipal) =
      obj match {
        case i: DefinedPrincipal => i.targets.toJson
        case WildcardPrincipal => JsString("*")
      }
    //TODO
    def read(json: JsValue) = ???
  }
}
case class DefinedPrincipal(targets: Map[String, Seq[String]]) extends PolicyPrincipal
case object WildcardPrincipal extends PolicyPrincipal

case class Policy(PolicyName: String, PolicyDocument: PolicyDocument)
object Policy extends DefaultJsonProtocol {
  implicit val format: JsonFormat[Policy] = jsonFormat2(Policy.apply)
}
case class PolicyDocument(Statement: Seq[PolicyStatement])
object PolicyDocument extends DefaultJsonProtocol {
  implicit val format: JsonFormat[PolicyDocument] = jsonFormat1(PolicyDocument.apply)
}