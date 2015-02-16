import java.io.File

import com.monsanto.arch.cloudformation.model._
import shapeless.HNil
import spray.json._
import org.scalatest._
import spray.json._

/**
 * Created by Ryan Richt on 1/18/15
 */


class CloudFormation_AT extends FunSpec with ShouldMatchers {

  describe("The condensation generator") {

    val handGenerated = new File(getClass.getResource("/cloudformation-template-vpc-example.json").toURI)
    val handContents = io.Source.fromFile(handGenerated).getLines().mkString("\n")
    val generatedContents = StaxTemplate.itsaDockerStack.toJson

        it("Should correctly recapitulate our template's AWSTemplateFormatVersion") {
          generatedContents.asJsObject.fields("AWSTemplateFormatVersion") should be(handContents.parseJson.asJsObject.fields("AWSTemplateFormatVersion"))
        }

        it("Should correctly recapitulate our template's Description") {
          generatedContents.asJsObject.fields("Description") should be(handContents.parseJson.asJsObject.fields("Description"))
        }

        it("Should correctly recapitulate our template's Parameters") {
          generatedContents.asJsObject.fields("Parameters") should be(handContents.parseJson.asJsObject.fields("Parameters"))
        }

        it("Should correctly recapitulate our template's Mappings") {
          generatedContents.asJsObject.fields("Mappings") should be(handContents.parseJson.asJsObject.fields("Mappings"))
        }

        it("Should correctly recapitulate our template's Resources") {
          generatedContents.asJsObject.fields("Resources") should be(handContents.parseJson.asJsObject.fields("Resources"))
        }

        it("Should correctly recapitulate our template's Outputs") {
          generatedContents.asJsObject.fields("Outputs") should be (handContents.parseJson.asJsObject.fields("Outputs"))
        }
  }
}

object StaxTemplate {

  def standardTagsNoNetworkPropagate(resourceName: String) = standardTagsNoNetwork(resourceName).map(_.copy(PropagateAtLaunch = Some(true)))

  def standardTagsNoNetwork(resourceName: String) = Seq(
    AmazonTag("Name", `Fn::Join`("-", Seq(resourceName, Ref("AWS::StackName")))),
    AmazonTag("App", Ref("App")),
    AmazonTag("Group", Ref("Group")),
    AmazonTag("Owner", Ref("Owner")),
    AmazonTag("Environment", Ref("Environment")),
    AmazonTag("KeepAlive", Ref("KeepAlive")),
    AmazonTag("CostCenter", Ref("CostCenter"))
  )

  def standardTags(resourceName: String, network: String) = AmazonTag("Network", network) +: standardTagsNoNetwork(resourceName)

  val vpcCidrParam = CidrIpParameter(
    name        = "VpcCidr",
    Description = "CIDR address range for the VPC to be created",
    Default     = CidrIp(10,183,0,0,16)
  )

  val allowSSHFromParam = CidrIpParameter(
    name        = "AllowSSHFrom",
    Description = "The net block (CIDR) that SSH is available to.",
    Default     = CidrIp(0,0,0,0,0)
  )

  val allowHTTPFromParam = CidrIpParameter(
    name        = "AllowHTTPFrom",
    Description = "The net block (CIDR) that can connect to the ELB.",
    Default     = CidrIp(0,0,0,0,0)
  )

  val vpcResource = `AWS::EC2::VPC`(
    "VPC",
    CidrBlock = ParameterRef(vpcCidrParam),
    Tags = standardTags("vpc", "Public")
  )

  val natRoleResource = `AWS::IAM::Role`("NATRole",
    AssumeRolePolicyDocument =
      PolicyDocument(
        Statement = Seq(
          PolicyStatement(
            Effect = "Allow",
            Principal = Some(DefinedPrincipal(Map("Service" -> Seq("ec2.amazonaws.com")))),
            Action = Seq("sts:AssumeRole")
          )
        )
      ),
    Path = "/",
    Policies = Seq(
      Policy(
        PolicyName = "NAT_Takeover",
        PolicyDocument =
          PolicyDocument(
            Statement = Seq(
              PolicyStatement(
                Effect = "Allow",
                Principal = None, // Did this ever work?
                Action = Seq("ec2:DescribeInstances", "ec2:DescribeRouteTables", "ec2:CreateRoute", "ec2:ReplaceRoute", "ec2:StartInstances", "ec2:StopInstances"),
                Resource = Some("*")
              )
            )
          )
      ),
      Policy(
        PolicyName = "StaxS3Access",
        PolicyDocument =
          PolicyDocument(
            Statement = Seq(
              PolicyStatement(
                Effect = "Allow",
                Action = Seq("s3:GetObject"),
                Resource = Some(`Fn::Join`("", Seq("arn:aws:s3:::", Ref("AWS::StackName"), "/*"))),
                Principal = None // Did this ever work?
              )
            )
          )
      )
    )
  )

  val publicSubnet1Param = CidrIpParameter(
    name        = "PublicSubnet1",
    Description = "CIDR address range for the public subnet to be created in the first AZ",
    Default     = CidrIp(10,183,1,0,24)
  )
  val privateSubnet1Param = CidrIpParameter(
    name        = "PrivateSubnet1",
    Description = "CIDR address range for the private subnet to be created in the first AZ",
    Default     = CidrIp(10,183,0,0,24)
  )

  val publicSubnet2Param = CidrIpParameter(
    name        = "PublicSubnet2",
    Description = "CIDR address range for the public subnet to be created in the second AZ",
    Default     = CidrIp(10,183,3,0,24)
  )
  val privateSubnet2Param = CidrIpParameter(
    name        = "PrivateSubnet2",
    Description = "CIDR address range for the private subnet to be created in the second AZ",
    Default     = CidrIp(10,183,2,0,24)
  )

  val itsaDockerStack = Template(
    AWSTemplateFormatVersion = "2010-09-09",
    Description = "Autoscaling group of Docker engines in dual AZ VPC with two NAT nodes in an active/active configuration. After successfully launching this CloudFormation stack, you will have 4 subnets in 2 AZs (a pair of public/private subnets in each AZ), a jump box, two NAT instances routing outbound traffic for their respective private subnets.  The NAT instances will automatically monitor each other and fix outbound routing problems if the other instance is unavailable.  The Docker engine autoscaling group will deploy to the private subnets.",
    Parameters = Some(
      Seq(
        StringParameter(
          name                  = "App",
          Description           = "Name for this ecosystem of services",
          MinLength             = 1,
          MaxLength             = 64,
          AllowedPattern        = Some("[-_ a-zA-Z0-9]*"),
          ConstraintDescription = Some("Can contain only alphanumeric characters, spaces, dashes and underscores."),
          Default               = "REPLACE APP"
        ),
        StringParameter(
          name                  = "Group",
          Description           = "Group responsible for this ecosystem of services",
          MinLength             = 1,
          MaxLength             = 64,
          AllowedPattern        = Some("[-_ a-zA-Z0-9]*"),
          ConstraintDescription = Some("Can contain only alphanumeric characters, spaces, dashes and underscores."),
          Default               = "REPLACE GROUP"
        ),
        StringParameter(
          name        = "ServiceDomain",
          Description = "Domain to register for services",
          MinLength   = 1,
          MaxLength   = 64,
          Default     = "REPLACE DOMAIN"
        ),
        StringParameter(
          name                  = "Owner",
          Description           = "Individual responsible for this ecosystem of services",
          MinLength             = 1,
          MaxLength             = 64,
          AllowedPattern        = Some("[-_ a-zA-Z0-9]*"),
          ConstraintDescription = Some("Can contain only alphanumeric characters, spaces, dashes and underscores."),
          Default               = "REPLACE OWNER"
        ),
        StringParameter(
          name                  = "Environment",
          Description           = "Description of deployment environment, e. g., test or production",
          MinLength             = 1,
          MaxLength             = 64,
          AllowedPattern        = Some("[-_ a-zA-Z0-9]*"),
          ConstraintDescription = Some("Can contain only alphanumeric characters, spaces, dashes and underscores."),
          Default               = "test"
        ),
        StringParameter(
          name                  = "KeepAlive",
          Description           = "Boolean to indicate whether to allow resource to be kept alive during nightly reaping",
          MinLength             = 4,
          MaxLength             = 5,
          AllowedValues         = Some(Seq("true", "false")),
          ConstraintDescription = Some("Value should be 'true' or 'false'"),
          Default               = "false"
        ),
        StringParameter(
          name                  = "CostCenter",
          Description           = "Cost center to be charged for this ecosystem of services",
          MinLength             = 18,
          MaxLength             = 18,
          AllowedPattern        = Some("\\d{4}-\\d{4}-[A-Z]{3}\\d{5}"),
          ConstraintDescription = Some("Format for cost center is ####-####-XYZ#####"),
          Default               = "0000-0000-ABC00000"
        ),
        StringParameter(
          name        = "DockerRegistryUrl",
          Description = "URL for private Docker Registry",
          MinLength   = 8,
          MaxLength   = 200,
          Default     = "https://index.docker.io/v1/"
        ),
        StringParameter(
          name        = "DockerRegistryUser",
          Description = "User name for private Docker Registry",
          MinLength   = 1,
          MaxLength   = 60,
          Default     = "nobody"
        ),
        StringParameter(
          name        = "DockerRegistryPass",
          Description = "Password for private Docker Registry",
          MinLength   = 1,
          MaxLength   = 60,
          Default     = "null"
        ),
        StringParameter(
          name        = "DockerRegistryEmail",
          Description = "Email address for private Docker Registry",
          MinLength   = 1,
          MaxLength   = 60,
          Default     = "nobody@null.com"
        ),
        `AWS::EC2::KeyPair::KeyName_Parameter`(
          name                  = "KeyName",
          Description           = "Name of an existing EC2 KeyPair to enable SSH access to the instances",
          ConstraintDescription = Some("Value must be a valid AWS key pair name in your account.")
        ),
        vpcCidrParam,
        publicSubnet1Param,
        privateSubnet1Param,
        publicSubnet2Param,
        privateSubnet2Param,
        StringParameter(
          name                  = "JumpInstanceType",
          Description           = "Instance type for public subnet jump nodes",
          AllowedValues         = Seq("m3.medium", "m3.large", "m3.xlarge", "m3.2xlarge", "c3.large","c3.xlarge", "c3.2xlarge", "c3.4xlarge","c3.8xlarge", "cc2.8xlarge","cr1.8xlarge","hi1.4xlarge", "hs1.8xlarge", "i2.xlarge", "i2.2xlarge", "i2.4xlarge", "i2.8xlarge", "r3.large", "r3.xlarge", "r3.2xlarge","r3.4xlarge", "r3.8xlarge", "t2.micro", "t2.small", "t2.medium"),
          ConstraintDescription = "Must be a valid EC2 instance type.",
          Default               = "t2.micro"
        ),
        StringParameter(
          name                  = "NATInstanceType",
          Description           = "Instance type for public subnet NAT nodes",
          AllowedValues         = Seq("m3.medium", "m3.large", "m3.xlarge", "m3.2xlarge", "c3.large","c3.xlarge", "c3.2xlarge", "c3.4xlarge","c3.8xlarge", "cc2.8xlarge","cr1.8xlarge","hi1.4xlarge", "hs1.8xlarge", "i2.xlarge", "i2.2xlarge", "i2.4xlarge", "i2.8xlarge", "r3.large", "r3.xlarge", "r3.2xlarge","r3.4xlarge", "r3.8xlarge", "t2.micro", "t2.small", "t2.medium"),
          ConstraintDescription = "Must be a valid EC2 instance type.",
          Default               = "t2.micro"
        ),
        StringParameter(
          name        = "NumberOfPings",
          Description = "The number of times the health check will ping the alternate NAT node",
          Default     = "3"
        ),
        StringParameter(
          name        = "PingTimeout",
          Description = "The number of seconds to wait for each ping response before determining that the ping has failed",
          Default     = "10"
        ),
        StringParameter(
          name        = "WaitBetweenPings",
          Description = "The number of seconds to wait between health checks",
          Default     = "2"
        ),
        StringParameter(
          name        = "WaitForInstanceStop",
          Description = "The number of seconds to wait for alternate NAT Node to stop before attempting to stop it again",
          Default     = "60"
        ),
        StringParameter(
          name        = "WaitForInstanceStart",
          Description = "The number of seconds to wait for alternate NAT node to restart before resuming health checks again",
          Default     = "300"
        ),
        StringParameter(
          name                  = "DockerInstanceType",
          Description           = "EC2 instance type for the Docker autoscaling group",
          AllowedValues         = Seq("m3.medium", "m3.large", "m3.xlarge", "m3.2xlarge", "c3.large","c3.xlarge", "c3.2xlarge", "c3.4xlarge","c3.8xlarge", "cc2.8xlarge","cr1.8xlarge","hi1.4xlarge", "hs1.8xlarge", "i2.xlarge", "i2.2xlarge", "i2.4xlarge", "i2.8xlarge", "r3.large", "r3.xlarge", "r3.2xlarge","r3.4xlarge", "r3.8xlarge", "t2.micro", "t2.small", "t2.medium"),
          ConstraintDescription = "Must be a valid EC2 HVM instance type.",
          Default               = "m3.medium"
        ),
        NumberParameter(
          name        = "ClusterSize",
          Description = "Number of nodes in cluster (2-12)",
          MinValue    = 3,
          MaxValue    = 12,
          Default     = 3
        ),
        NumberParameter(
          name        = "RouterClusterSize",
          Description = "Number of nodes in cluster (2-12)",
          MinValue    = 2,
          MaxValue    = 12,
          Default     = 2
        ),
        NumberParameter(
          name        = "AutoScaleCooldown",
          Description = "Time in seconds between autoscaling events",
          MinValue    = 60,
          MaxValue    = 3600,
          Default     = 300
        ),
        StringParameter(
          name                  = "CoreOSChannelAMI",
          Description           = "MapName for the update channel AMI to use when launching CoreOS instances",
          AllowedValues         = Seq("CoreOSStableAMI","CoreOSBetaAMI","CoreOSAlphaAMI"),
          ConstraintDescription = "Value should be 'CoreOSStableAMI', 'CoreOSBetaAMI', or 'CoreOSAlphaAMI'",
          Default               = "CoreOSStableAMI"
        ),
        StringParameter(
          name        = "DiscoveryURL",
          Description = "An unique etcd cluster discovery URL. Grab a new token from https://discovery.etcd.io/new"
        ),
        StringParameter(
          name          = "AdvertisedIPAddress",
          Description   = "Use 'private' if your etcd cluster is within one region or 'public' if it spans regions or cloud providers.",
          AllowedValues = Seq("private","public"),
          Default       = "private"
        ),
        allowHTTPFromParam,
        allowSSHFromParam
      )
    ),

    Mappings = Some(
      Seq(
        StringMapping(
          "CoreOSAlphaAMI",
          Map(
              "us-east-1" -> Map("AMI" -> "ami-52396c3a"),
              "us-west-1" -> Map("AMI" -> "ami-240f1561"),
              "us-west-2" -> Map("AMI" -> "ami-6d85a15d"),
              "eu-west-1" -> Map("AMI" -> "ami-f56ee482")
             )
        ),
        StringMapping(
          "CoreOSBetaAMI",
          Map(
            "us-east-1" -> Map("AMI" -> "ami-509bd838"),
            "us-west-1" -> Map("AMI" -> "ami-4ab7af0f"),
            "us-west-2" -> Map("AMI" -> "ami-07762d37"),
            "eu-west-1" -> Map("AMI" -> "ami-19af216e")
          )
        ),
        StringMapping(
          "CoreOSStableAMI",
          Map(
            "us-east-1" -> Map("AMI" -> "ami-8297d4ea"),
            "us-west-1" -> Map("AMI" -> "ami-24b5ad61"),
            "us-west-2" -> Map("AMI" -> "ami-f1702bc1"),
            "eu-west-1" -> Map("AMI" -> "ami-5d911f2a")
          )
        ),
        StringMapping(
          "AmazonLinuxAMI",
          Map(
              "us-east-1" -> Map("AMI" -> "ami-146e2a7c"),
              "us-west-1" -> Map("AMI" -> "ami-42908907"),
              "us-west-2" -> Map("AMI" -> "ami-dfc39aef"),
              "eu-west-1" -> Map("AMI" -> "ami-9d23aeea")
             )
        ),
        StringMapping(
          "AWSNATAMI",
          Map(
              "us-east-1" -> Map("AMI" -> "ami-184dc970"),
              "us-west-1" -> Map("AMI" -> "ami-a98396ec"),
              "us-west-2" -> Map("AMI" -> "ami-290f4119"),
              "eu-west-1" -> Map("AMI" -> "ami-14913f63")
          )
        )
      )
    ),

    Resources = Some(
      Seq(
        natRoleResource,
      `AWS::IAM::InstanceProfile`(
                                   "NATRoleProfile",
                                   Path = "/",
                                   Roles = Seq(ResourceRef(natRoleResource))
      ),
      vpcResource,
      `AWS::EC2::Subnet`(
                          "PubSubnet1",
                          VpcId = ResourceRef(vpcResource),
                          AvailabilityZone = "us-east-1a",
                          CidrBlock = ParameterRef(publicSubnet1Param),
                          Tags = standardTags("pubsubnet1", "Public")
                        ),
      `AWS::EC2::Subnet`(
                          "PriSubnet1",
                          VpcId = ResourceRef(vpcResource),
                          AvailabilityZone = "us-east-1a",
                          CidrBlock = ParameterRef(privateSubnet1Param),
                          Tags = standardTags("prisubnet1", "Private")
                        ),
      `AWS::EC2::Subnet`(
                          "PubSubnet2",
                          VpcId = ResourceRef(vpcResource),
                          AvailabilityZone = "us-east-1b",
                          CidrBlock = ParameterRef(publicSubnet2Param),
                          Tags = standardTags("pubsubnet2", "Public")
                        ),
      `AWS::EC2::Subnet`(
                          "PriSubnet2",
                          VpcId = ResourceRef(vpcResource),
                          AvailabilityZone = "us-east-1b",
                          CidrBlock = ParameterRef(privateSubnet2Param),
                          Tags = standardTags("prisubnet2", "Private")
                        ),
      `AWS::EC2::InternetGateway`(
                                   "InternetGateway",
                                   Tags = standardTags("igw", "Public")
                                 ),
      `AWS::EC2::VPCGatewayAttachment`(
                                        "GatewayToInternet",
                                        VpcId = ResourceRef(vpcResource),
                                        InternetGatewayId = Ref("InternetGateway")
                                        ),
      `AWS::EC2::RouteTable`(
                              "PublicRouteTable",
                              VpcId = ResourceRef(vpcResource),
                              Tags = standardTags("pubrt", "Public")
                            ),
      `AWS::EC2::Route`(
                         "PublicRouteTableRoute1",
                         RouteTableId = Ref("PublicRouteTable"),
                         DestinationCidrBlock = "0.0.0.0/0",
                         GatewayId = Some(Ref("InternetGateway"))
                        ),
      `AWS::EC2::RouteTable`(
                              "PrivateRouteTable1",
                              VpcId = ResourceRef(vpcResource),
                              Tags = standardTags("privrt1", "Private")
                            ),
      `AWS::EC2::Route`(
                         "PrivateRouteTable1Route1",
                          RouteTableId = Ref("PrivateRouteTable1"),
                          DestinationCidrBlock = "0.0.0.0/0",
                          InstanceId = Some(Ref("NAT1Instance"))
                       ),
      `AWS::EC2::RouteTable`(
                              "PrivateRouteTable2",
                              VpcId = ResourceRef(vpcResource),
                              Tags = standardTags("privrt2", "Private")
                            ),
      `AWS::EC2::Route`(
                         "PrivateRouteTable2Route1",
                          RouteTableId = Ref("PrivateRouteTable2"),
                          DestinationCidrBlock = "0.0.0.0/0",
                          InstanceId = Some(Ref("NAT2Instance"))
                        ),
      `AWS::EC2::SubnetRouteTableAssociation`(
                                               "PubSubnet1RTAssoc",
                                                SubnetId = Ref("PubSubnet1"),
                                                RouteTableId = Ref("PublicRouteTable")
                                             ),
      `AWS::EC2::SubnetRouteTableAssociation`(
                                               "PubSubnet2RTAssoc",
                                                SubnetId = Ref("PubSubnet2"),
                                                RouteTableId = Ref("PublicRouteTable")
                                             ),
      `AWS::EC2::SubnetRouteTableAssociation`(
                                               "PriSubnet1RTAssoc",
                                               SubnetId = Ref("PriSubnet1"),
                                               RouteTableId = Ref("PrivateRouteTable1")
                                              ),
      `AWS::EC2::SubnetRouteTableAssociation`(
                                               "PriSubnet2RTAssoc",
                                               SubnetId = Ref("PriSubnet2"),
                                               RouteTableId = Ref("PrivateRouteTable2")
                                             ),
      `AWS::EC2::SecurityGroup`(
                                 "JumpSecurityGroup",
                                 GroupDescription = "Rules for allowing access to public subnet nodes",
                                 VpcId = ResourceRef(vpcResource),
                                 SecurityGroupEgress = None,
                                 SecurityGroupIngress =
                                   Some(Seq(
                                     CidrIngressSpec(
                                                  IpProtocol = "tcp",
                                                  CidrIp = ParameterRef(allowSSHFromParam),
                                                  FromPort = "22",
                                                  ToPort = "22"
                                                )
                                   )),
                                 Tags = standardTagsNoNetwork("jumpsg")
                               ),
      `AWS::EC2::Instance`(
                            "JumpInstance",
                            InstanceType = Ref("JumpInstanceType"),
                            KeyName = Ref("KeyName"),
                            SubnetId = Ref("PubSubnet1"),
                            ImageId = `Fn::FindInMap`("AmazonLinuxAMI", Ref("AWS::Region"), "AMI"),
                            SecurityGroupIds = Seq( Ref("JumpSecurityGroup")),
                            Tags = standardTagsNoNetwork("jump")
                          ),
      `AWS::EC2::EIP`(
                       "JumpEIP",
                       Domain = "vpc",
                       InstanceId = Ref("JumpInstance")
                     ),
      `AWS::EC2::SecurityGroup`(
                                 "NATSecurityGroup",
                                 GroupDescription = "Rules for allowing access to public subnet nodes",
                                 VpcId = ResourceRef(vpcResource),
                                 SecurityGroupIngress = Some(Seq(
                                   SGIngressSpec(
                                                  IpProtocol = "tcp",
                                                  SourceSecurityGroupId = Ref("JumpSecurityGroup"),
                                                  FromPort = "22",
                                                  ToPort = "22"
                                              ),
                                   CidrIngressSpec(
                                                    IpProtocol = "-1",
                                                    CidrIp = ParameterRef(vpcCidrParam),
                                                    FromPort = "0",
                                                    ToPort = "65535"
                                                  )
                                 )),
                                 SecurityGroupEgress = Some(Seq(
                                   CidrEgressSpec(
                                                   IpProtocol = "-1",
                                                   CidrIp = CidrIp(0,0,0,0,0),
                                                   FromPort = "0",
                                                   ToPort = "65535"
                                                 )
                                 )),
                                 Tags = standardTagsNoNetwork("natsg")
                               ),
      `AWS::EC2::SecurityGroupIngress`(
                                        "NATSecurityGroupAllowICMP",
                                        GroupId = Ref("NATSecurityGroup"),
                                        IpProtocol = "icmp",
                                        SourceSecurityGroupId = Ref("NATSecurityGroup"),
                                        FromPort = "-1",
                                        ToPort = "-1"
                                      ),
      `AWS::EC2::Instance`(
                            "NAT1Instance",
                            Metadata = Some(Map("Comment1" -> "Create NAT #1")),
                            InstanceType = Ref("NATInstanceType"),
                            KeyName = Ref("KeyName"),
                            IamInstanceProfile = Some(Ref("NATRoleProfile")),
                            SubnetId = Ref("PubSubnet1"),
                            SourceDestCheck = Some("false"),
                            ImageId = `Fn::FindInMap`("AWSNATAMI", Ref("AWS::Region"), "AMI"),
                            SecurityGroupIds = Seq( Ref("NATSecurityGroup") ),
                            Tags = standardTagsNoNetwork("nat1"),
                            UserData = Some(`Fn::Base64`(
                              `Fn::Join`("", 
                                         Seq(
                                           "#!/bin/bash -v\n",
                                           "yum update -y aws*\n",
                                           ". /etc/profile.d/aws-apitools-common.sh\n",
                                           "cd /root\n",
                                           "aws s3 cp s3://",
                                           Ref("AWS::StackName"),
                                           "/user-data-nat.sh user-data-nat.sh\n",
                                           "/bin/bash user-data-nat.sh",
                                           " ", Ref("PrivateRouteTable2"),
                                           " ", Ref("PrivateRouteTable1"),
                                           " ", Ref("AWS::Region"),
                                           " ", Ref("AWS::StackName"), "\n",
                                           "# EOF\n"
                                         )
                                        )
                            ))
                          ),
    
      `AWS::EC2::EIP`(
                       "NAT1EIP",
                       Domain = "vpc",
                       InstanceId = Ref("NAT1Instance")
      ),
    
      `AWS::EC2::Instance`(
                            "NAT2Instance",
                            Metadata = Some(Map("Comment1" -> "Create NAT #2")),
                            InstanceType = Ref("NATInstanceType"),
                            KeyName = Ref("KeyName"),
                            IamInstanceProfile = Some(Ref("NATRoleProfile")),
                            SubnetId = Ref("PubSubnet2"),
                            SourceDestCheck = Some("false"),
                            ImageId = `Fn::FindInMap`("AWSNATAMI", Ref("AWS::Region"),"AMI"),
                            SecurityGroupIds = Seq( Ref("NATSecurityGroup")),
                            Tags = standardTagsNoNetwork("nat2"),
                            UserData = Some(`Fn::Base64`(
                              `Fn::Join`(
                                          "",
                                          Seq(
                                            "#!/bin/bash -v\n",
                                            "yum update -y aws*\n",
                                            ". /etc/profile.d/aws-apitools-common.sh\n",
                                            "cd /root\n",
                                            "aws s3 cp s3://",
                                            Ref("AWS::StackName"),
                                            "/user-data-nat.sh user-data-nat.sh\n",
                                            "/bin/bash user-data-nat.sh",
                                            " ", Ref("PrivateRouteTable1"),
                                            " ", Ref("PrivateRouteTable2"),
                                            " ", Ref("AWS::Region"),
                                            " ", Ref("AWS::StackName"), "\n",
                                            "# EOF\n"
                                          )
                                        )
                            ))
      ),
    
      `AWS::EC2::EIP`(
                       "NAT2EIP",
                        Domain = "vpc",
                        InstanceId = Ref("NAT2Instance")
                     ),
    
      `AWS::EC2::SecurityGroup`(
                                 "RouterELBSecurityGroup",
                                  GroupDescription = "Rules for allowing access to/from service router ELB",
                                  VpcId = ResourceRef(vpcResource),
                                 SecurityGroupEgress = None,
                                  SecurityGroupIngress = Some(Seq(
                                    CidrIngressSpec(
                                                     IpProtocol = "tcp",
                                                     CidrIp = ParameterRef(allowHTTPFromParam),
                                                     FromPort = "80",
                                                     ToPort = "80"
                                                   ),
                                    CidrIngressSpec(
                                                    IpProtocol = "tcp",
                                                    CidrIp = ParameterRef(allowHTTPFromParam),
                                                    FromPort = "443",
                                                    ToPort = "443"
                                                   )
                                  )),
                                 Tags = standardTagsNoNetwork("router-elbsg")
                                 ),

      `AWS::EC2::SecurityGroupEgress`(
                                       "RouterELBToRouterCoreOSRouter",
                                       GroupId = Ref("RouterELBSecurityGroup"),
                                       IpProtocol = "tcp",
                                       DestinationSecurityGroupId = Ref("RouterCoreOSSecurityGroup"),
                                       FromPort = "80",
                                       ToPort = "80"
                                     ),
      `AWS::EC2::SecurityGroupEgress`(
                                       "RouterELBToRouterCoreOSELB",
                                       GroupId = Ref("RouterELBSecurityGroup"),
                                       IpProtocol = "tcp",
                                       DestinationSecurityGroupId = Ref("RouterCoreOSSecurityGroup"),
                                       FromPort = "4001",
                                       ToPort = "4001"
                                     ),
      `AWS::ElasticLoadBalancing::LoadBalancer`(
                                                  "RouterELB",
                                                  CrossZone = true,
                                                  SecurityGroups = Seq(Ref("RouterELBSecurityGroup")),
                                                  Subnets = Seq(Ref("PubSubnet1"), Ref("PubSubnet2")),
                                                  Listeners = Seq(
                                                    Listener(
                                                              LoadBalancerPort = "80",
                                                              InstancePort = "80",
                                                              Protocol = "HTTP"
                                                            )
                                                  ),
                                                  HealthCheck = HealthCheck(
                                                                            Target = "HTTP:4001/version",
                                                                            HealthyThreshold = "3",
                                                                            UnhealthyThreshold = "5",
                                                                            Interval = "30",
                                                                            Timeout = "5"
                                                                           ),
                                                 Tags = standardTagsNoNetwork("router-elb")
      ),

      `AWS::EC2::SecurityGroup`(
                                 "CoreOSFromJumpSecurityGroup",
                                 GroupDescription = "Allow general CoreOS/Docker access from the jump box",
                                 VpcId = ResourceRef(vpcResource),
                                 SecurityGroupEgress = None,
                                 SecurityGroupIngress = Some(Seq(
                                   SGIngressSpec(
                                                  IpProtocol = "tcp",
                                                  SourceSecurityGroupId = Ref
                                                    ("JumpSecurityGroup"),
                                                  FromPort = "22",
                                                  ToPort = "22"
                                                ),
                                   SGIngressSpec(
                                                  IpProtocol = "tcp",
                                                  SourceSecurityGroupId = Ref("JumpSecurityGroup"),
                                                  FromPort = "80",
                                                  ToPort = "80"
                                                ),
                                   SGIngressSpec(
                                                  IpProtocol = "tcp",
                                                  SourceSecurityGroupId = Ref("JumpSecurityGroup"),
                                                  FromPort = "4001",
                                                  ToPort = "4001"
                                                )
                                 )),
                                 Tags = standardTagsNoNetwork("coreos-from-jumpsg")
                               ),

      `AWS::EC2::SecurityGroup`(
                                 "RouterCoreOSSecurityGroup",
                                 GroupDescription = "Router CoreOS SecurityGroup",
                                 VpcId = ResourceRef(vpcResource),
                                 SecurityGroupIngress = Some(Seq(
                                    SGIngressSpec(
                                                   IpProtocol = "tcp",
                                                   SourceSecurityGroupId = Ref
                                                     ("RouterELBSecurityGroup"),
                                                   FromPort = "80",
                                                   ToPort = "80"
                                                 ),
                                    SGIngressSpec(
                                                   IpProtocol = "tcp",
                                                   SourceSecurityGroupId = Ref("RouterELBSecurityGroup"),
                                                   FromPort = "4001",
                                                   ToPort = "4001"
                                                 )
                                 )),
                                 SecurityGroupEgress = None,
                                 Tags = standardTagsNoNetwork("routersg")
                               ),
    
      `AWS::EC2::SecurityGroupIngress`(
                                        "RouterCoreOSFromRouterCoreOS",
                                        GroupId = Ref("RouterCoreOSSecurityGroup"),
                                        IpProtocol = "-1",
                                        SourceSecurityGroupId = Ref("RouterCoreOSSecurityGroup"),
                                        FromPort = "0",
                                        ToPort = "65535"
                                      ),
    
      `AWS::EC2::SecurityGroupIngress`(
                                        "RouterCoreOSFromCoreOS",
                                        GroupId = Ref("RouterCoreOSSecurityGroup"),
                                        IpProtocol = "-1",
                                        SourceSecurityGroupId = Ref("CoreOSSecurityGroup"),
                                        FromPort = "0",
                                        ToPort = "65535"
                                      ),

      `AWS::AutoScaling::LaunchConfiguration`(
                                               "RouterCoreOSServerLaunchConfig",
                                               ImageId = `Fn::FindInMap`(Ref("CoreOSChannelAMI"), Ref("AWS::Region"), "AMI"),
                                               InstanceType = Ref("DockerInstanceType"),
                                               KeyName = Ref("KeyName"),
                                               SecurityGroups = Seq(Ref("RouterCoreOSSecurityGroup"), Ref("CoreOSFromJumpSecurityGroup")),
                                               UserData = `Fn::Base64`(
                                                 `Fn::Join`(
                                                   "",
                                                   Seq(
                                                     "#cloud-config\n\n",
                                                     "coreos:\n",
                                                     "  etcd:\n",
                                                     "    discovery: ", Ref("DiscoveryURL"), "\n",
                                                     "    addr: $", Ref("AdvertisedIPAddress"), "_ipv4:4001\n",
                                                     "    peer-addr: $", Ref("AdvertisedIPAddress"), "_ipv4:7001\n",
                                                     "  units:\n",
                                                     "    - name: etcd.service\n",
                                                     "      command: start\n",
                                                     "    - name: consul.service\n",
                                                     "      command: start\n",
                                                     "      content: |\n",
                                                     "        [Unit]\n",
                                                     "        Description=Consul Agent\n",
                                                     "        After=docker.service\n",
                                                     "        After=etcd.service\n",
                                                     "        [Service]\n",
                                                     "        Restart=on-failure\n",
                                                     "        RestartSec=240\n",
                                                     "        ExecStartPre=-/usr/bin/docker kill consul\n",
                                                     "        ExecStartPre=-/usr/bin/docker rm consul\n",
                                                     "        ExecStartPre=/usr/bin/docker pull progrium/consul\n",
                                                     "        ExecStart=/usr/bin/docker run -h %H --name consul -p 8300:8300 -p 8301:8301 -p 8301:8301/udp -p 8302:8302 -p 8302:8302/udp -p 8400:8400 -p 8500:8500 -p 53:53/udp -e SERVICE_IGNORE=true progrium/consul -advertise $", Ref("AdvertisedIPAddress"), "_ipv4\n",
                                                     "        ExecStop=/usr/bin/docker stop consul\n",
                                                     "    - name: consul-announce.service\n",
                                                     "      command: start\n",
                                                     "      content: |\n",
                                                     "        [Unit]\n",
                                                     "        Description=Consul Server Announcer\n",
                                                     "        PartOf=consul.service\n",
                                                     "        After=consul.service\n",
                                                     "        [Service]\n",
                                                     "        ExecStart=/bin/sh -c \"while true; do etcdctl set /consul/bootstrap/machines/$(cat /etc/machine-id) $", Ref("AdvertisedIPAddress"), "_ipv4 --ttl 60; /usr/bin/docker exec consul consul join $(etcdctl ls /consul/bootstrap/machines | xargs -n 1 etcdctl get | tr '\\n' ' '); sleep 45; done\"\n",
                                                     "        ExecStop=/bin/sh -c \"/usr/bin/etcdctl rm /consul/bootstrap/machines/$(cat /etc/machine-id)\"\n",
                                                     "    - name: docker-login.service\n",
                                                     "      command: start\n",
                                                     "      content: |\n",
                                                     "        [Unit]\n",
                                                     "        Description=Log in to private Docker Registry\n",
                                                     "        After=docker.service\n",
                                                     "        [Service]\n",
                                                     "        Type=oneshot\n",
                                                     "        RemainAfterExit=yes\n",
                                                     "        ExecStart=/usr/bin/docker login -e ", Ref("DockerRegistryEmail"), " -u ", Ref("DockerRegistryUser"), " -p ", Ref("DockerRegistryPass"), " ", Ref("DockerRegistryUrl"), "\n",
                                                     "        ExecStop=/usr/bin/docker logout ", Ref("DockerRegistryUrl"), "\n",
                                                     "    - name: axon-router.service\n",
                                                     "      command: start\n",
                                                     "      content: |\n",
                                                     "        [Unit]\n",
                                                     "        Description=Run axon-router\n",
                                                     "        After=docker.service\n",
                                                     "        Requires=docker.service\n\n",
                                                     "        [Service]\n",
                                                     "        Restart=always\n",
                                                     "        ExecStartPre=-/usr/bin/docker kill axon-router\n",
                                                     "        ExecStartPre=-/usr/bin/docker rm axon-router\n",
                                                     "        ExecStartPre=/usr/bin/docker pull monsantoco/axon-router:latest\n",
                                                     "        ExecStart=/usr/bin/docker run -t -e \"NS_IP=172.17.42.1\" --name axon-router -p 80:80 monsantoco/axon-router:latest\n",
                                                     "        ExecStop=/usr/bin/docker stop axon-router\n",
                                                     "    - name: settimezone.service\n",
                                                     "      command: start\n",
                                                     "      content: |\n",
                                                     "        [Unit]\n",
                                                     "        Description=Set the timezone\n",
                                                     "        [Service]\n",
                                                     "        ExecStart=/usr/bin/timedatectl set-timezone UTC\n",
                                                     "        RemainAfterExit=yes\n",
                                                     "        Type=oneshot\n",
                                                     "write_files:\n",
                                                     "  - path: /etc/ntp.conf\n",
                                                     "    content: |\n",
                                                     "      server 0.pool.ntp.org\n",
                                                     "      server 1.pool.ntp.org\n",
                                                     "      server 2.pool.ntp.org\n",
                                                     "      server 3.pool.ntp.org\n",
                                                     "      restrict default nomodify nopeer noquery limited kod\n",
                                                     "      restrict 127.0.0.1\n"
                                                   )
                                                 )
                                               )
                                             ),

      `AWS::AutoScaling::AutoScalingGroup`(
                                            "RouterCoreOSServerAutoScale",
                                            AvailabilityZones = Seq("us-east-1a", "us-east-1b"),
                                            LaunchConfigurationName = Ref("RouterCoreOSServerLaunchConfig"),
                                            LoadBalancerNames = Some(Seq(Ref("RouterELB"))),
                                            MinSize = "2",
                                            MaxSize = "12",
                                            DesiredCapacity = Ref("RouterClusterSize"),
                                            HealthCheckType = "EC2",
                                            VPCZoneIdentifier = Seq(Ref("PriSubnet1"), Ref("PriSubnet2")),
                                            Tags = standardTagsNoNetworkPropagate("router")
                                          ),

      `AWS::EC2::SecurityGroup`(
                                 "CoreOSSecurityGroup",
                                 GroupDescription = "Security Group for microservices CoreOS Auto Scaling Group",
                                 VpcId = ResourceRef(vpcResource),
                                 SecurityGroupIngress = Some(Seq(
                                     SGIngressSpec(
                                       IpProtocol = "-1",
                                       SourceSecurityGroupId = Ref("RouterCoreOSSecurityGroup"),
                                       FromPort = "0",
                                       ToPort = "65535"
                                     )
                                   )
                                 ),
                                 SecurityGroupEgress = None,
                                 Tags = standardTagsNoNetwork("containersg")
                               ),

      `AWS::EC2::SecurityGroupIngress`(
                                        "CoreOSFromCoreOS",
                                        GroupId = Ref("CoreOSSecurityGroup"),
                                        IpProtocol = "-1",
                                        SourceSecurityGroupId = Ref("CoreOSSecurityGroup"),
                                        FromPort = "0",
                                        ToPort = "65535"
                                      ),

      `AWS::AutoScaling::LaunchConfiguration`(
                                               "CoreOSServerLaunchConfig",
                                               ImageId = `Fn::FindInMap`(Ref("CoreOSChannelAMI"), Ref("AWS::Region"), "AMI"),
                                               InstanceType = Ref("DockerInstanceType"),
                                               KeyName = Ref("KeyName"),
                                               SecurityGroups = Seq( Ref("CoreOSSecurityGroup"), Ref("CoreOSFromJumpSecurityGroup") ),
      UserData = `Fn::Base64`(
        `Fn::Join`(
          "",
          Seq(
            "#cloud-config\n\n",
            "coreos:\n",
            "  etcd:\n",
            "    discovery: ", Ref("DiscoveryURL"), "\n",
            "    addr: $", Ref("AdvertisedIPAddress"), "_ipv4:4001\n",
            "    peer-addr: $", Ref("AdvertisedIPAddress"), "_ipv4:7001\n",
            "  units:\n",
            "    - name: etcd.service\n",
            "      command: start\n",
            "    - name: fleet.service\n",
            "      command: start\n",
            "    - name: fleet.socket\n",
            "      command: start\n",
            "      enable: yes\n",
            "      content: |\n",
            "        [Unit]\n",
            "        Description=Fleet Socket for the API\n",
            "        [Socket]\n",
            "        ListenStream=49153\n",
            "        BindIPv6Only=both\n",
            "        Service=fleet.service\n\n",
            "        [Install]\n",
            "        WantedBy=sockets.target\n",
            "    - name: consul.service\n",
            "      command: start\n",
            "      content: |\n",
            "        [Unit]\n",
            "        Description=Consul Server\n",
            "        After=docker.service\n",
            "        After=etcd.service\n",
            "        After=fleet.service\n",
            "        [Service]\n",
            "        Restart=on-failure\n",
            "        RestartSec=240\n",
            "        ExecStartPre=-/usr/bin/docker kill consul\n",
            "        ExecStartPre=-/usr/bin/docker rm consul\n",
            "        ExecStartPre=/usr/bin/docker pull progrium/consul\n",
            "        ExecStart=/bin/bash -c \"eval $(/usr/bin/docker run --rm progrium/consul cmd:run $", Ref("AdvertisedIPAddress"), "_ipv4 -e SERVICE_IGNORE=true)\"\n",
            "        ExecStop=/usr/bin/docker stop consul\n",
            "    - name: consul-announce.service\n",
            "      command: start\n",
            "      content: |\n",
            "        [Unit]\n",
            "        Description=Consul Server Announcer\n",
            "        PartOf=consul.service\n",
            "        After=consul.service\n",
            "        [Service]\n",
            "        ExecStart=/bin/sh -c \"while true; do etcdctl set /consul/bootstrap/machines/$(cat /etc/machine-id) $", Ref("AdvertisedIPAddress"), "_ipv4 --ttl 60; /usr/bin/docker exec consul consul join $(etcdctl ls /consul/bootstrap/machines | xargs -n 1 etcdctl get | tr '\\n' ' '); sleep 45; done\"\n",
            "        ExecStop=/bin/sh -c \"/usr/bin/etcdctl rm /consul/bootstrap/machines/$(cat /etc/machine-id)\"\n",
            "    - name: docker-login.service\n",
            "      command: start\n",
            "      content: |\n",
            "        [Unit]\n",
            "        Description=Log in to private Docker Registry\n",
            "        After=docker.service\n",
            "        [Service]\n",
            "        Type=oneshot\n",
            "        RemainAfterExit=yes\n",
            "        ExecStart=/usr/bin/docker login -e ", Ref("DockerRegistryEmail"), " -u ", Ref("DockerRegistryUser"), " -p ", Ref("DockerRegistryPass"), " ", Ref("DockerRegistryUrl"), "\n",
            "        ExecStop=/usr/bin/docker logout ", Ref("DockerRegistryUrl"), "\n",
            "# Run registrator\n",
            "    - name: registrator.service\n",
            "      command: start\n",
            "      content: |\n",
            "        [Unit]\n",
            "        Description=Run registrator\n",
            "        After=docker.service\n",
            "        Requires=docker.service\n\n",
            "        [Service]\n",
            "        Restart=always\n",
            "        ExecStartPre=-/usr/bin/docker kill registrator\n",
            "        ExecStartPre=-/usr/bin/docker rm registrator\n",
            "        ExecStartPre=/usr/bin/docker pull progrium/registrator:latest\n",
            "        ExecStart=/usr/bin/docker run --name registrator -v /var/run/docker.sock:/tmp/docker.sock -h %H progrium/registrator:latest consul://$", Ref("AdvertisedIPAddress"), "_ipv4:8500\n",
            "         ExecStop=/usr/bin/docker stop registrator\n",
            "# Run axon-router\n",
            "    - name: axon-router.service\n",
            "      command: start\n",
            "      content: |\n",
            "        [Unit]\n",
            "        Description=Run axon-router\n",
            "        After=docker.service\n",
            "        Requires=docker.service\n\n",
            "        [Service]\n",
            "        Restart=always\n",
            "        ExecStartPre=-/usr/bin/docker kill axon-router\n",
            "        ExecStartPre=-/usr/bin/docker rm axon-router\n",
            "        ExecStartPre=/usr/bin/docker pull monsantoco/axon-router:latest\n",
            "        ExecStart=/usr/bin/docker run -t -e \"NS_IP=172.17.42.1\" --name axon-router -p 80:80 monsantoco/axon-router:latest\n",
            "        ExecStop=/usr/bin/docker stop axon-router\n",
            "    - name: settimezone.service\n",
            "      command: start\n",
            "      content: |\n",
            "        [Unit]\n",
            "        Description=Set the timezone\n",
            "        [Service]\n",
            "        ExecStart=/usr/bin/timedatectl set-timezone UTC\n",
            "        RemainAfterExit=yes\n",
            "        Type=oneshot\n",
            "write_files:\n",
            "  - path: /etc/ntp.conf\n",
            "    content: |\n",
            "      server 0.pool.ntp.org\n",
            "      server 1.pool.ntp.org\n",
            "      server 2.pool.ntp.org\n",
            "      server 3.pool.ntp.org\n",
            "      restrict default nomodify nopeer noquery limited kod\n",
            "      restrict 127.0.0.1\n"
         )
        )
       )
      ),

      `AWS::AutoScaling::AutoScalingGroup`(
                                            "CoreOSServerAutoScale",
                                            AvailabilityZones = Seq("us-east-1a", "us-east-1b"),
                                            LaunchConfigurationName = Ref("CoreOSServerLaunchConfig"),
                                            MinSize = "2",
                                            MaxSize = "12",
                                            DesiredCapacity = Ref("ClusterSize"),
                                            HealthCheckType = "EC2",
                                            VPCZoneIdentifier = Seq(Ref("PriSubnet1"), Ref("PriSubnet2")),
                                            Tags = standardTagsNoNetworkPropagate("container"),
                                            LoadBalancerNames = None
                                          ),

      `AWS::AutoScaling::ScalingPolicy`(
                                         "CoreOSServerAutoScaleUpPolicy",
                                         AdjustmentType = "ChangeInCapacity",
                                         AutoScalingGroupName = Ref("CoreOSServerAutoScale"),
                                         Cooldown = Ref("AutoScaleCooldown"),
                                         ScalingAdjustment = "1"
                                       ),

      `AWS::AutoScaling::ScalingPolicy`(
                                         "CoreOSServerAutoScaleDownPolicy",
                                         AdjustmentType = "ChangeInCapacity",
                                         AutoScalingGroupName = Ref("CoreOSServerAutoScale"),
                                         Cooldown = Ref("AutoScaleCooldown"),
                                         ScalingAdjustment = "-1"
                                       )
    )
    ),

    Outputs = Some(
      Seq(
        Output("VPCID",          "VPC Info",          ResourceRef(vpcResource)                               ),
        Output("JumpEIP",        "Jump Box EIP",      Ref("JumpEIP")                           ),
        Output("NAT1EIP",        "NAT 1 EIP",         Ref("NAT1EIP")                           ),
        Output("NAT2EIP",        "NAT 2 EIP",         Ref("NAT2EIP")                           ),
        Output("RouterDNS",      "ELB DNS Name",      `Fn::GetAtt`(Seq("RouterELB","DNSName")) ),
        Output("PublicSubnet1",  "Public Subnet #1",  Ref("PubSubnet1")                        ),
        Output("PrivateSubnet1", "Private Subnet #1", Ref("PriSubnet1")                        ),
        Output("PublicSubnet2",  "Public Subnet #2",  Ref("PubSubnet2")                        ),
        Output("PrivateSubnet2", "Private Subnet #2", Ref("PriSubnet2")                        )
      )
    )
  )
}