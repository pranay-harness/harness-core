package software.wings.cloudprovider.aws;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INIT_TIMEOUT;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.service.impl.aws.model.AwsConstants.MAIN_ECS_CONTAINER_NAME_TAG;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.Deployment;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.NetworkInterface;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceEvent;
import com.amazonaws.services.ecs.model.Tag;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.TaskDefinitionField;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TimeoutException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.cloudprovider.UpdateServiceCountRequestData;
import software.wings.cloudprovider.aws.EcsContainerDetails.EcsContainerDetailsBuilder;
import software.wings.service.impl.AwsHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 12/28/16.
 */
@Singleton
@Slf4j
public class EcsContainerServiceImpl implements EcsContainerService {
  @Inject private AwsHelperService awsHelperService = new AwsHelperService();
  @Inject private TimeLimiter timeLimiter;
  @Inject private AwsMetadataApiHelper awsMetadataApiHelper;

  private ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  /**
   * Create cluster.
   */
  public void createCluster() {
    awsHelperService.createStack("us-east-1",
        new CreateStackRequest()
            .withStackName("EC2ContainerService-demo")
            .withTemplateBody("AWSTemplateFormatVersion: '2010-09-09'\n"
                + "Description: >\n"
                + "  AWS CloudFormation template to create a new VPC\n"
                + "  or use an existing VPC for ECS deployment\n"
                + "  in Create Cluster Wizard\n"
                + "Parameters:\n"
                + "  EcsClusterName:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Specifies the ECS Cluster Name with which the resources would be\n"
                + "      associated\n"
                + "    Default: default\n"
                + "  EcsAmiId:\n"
                + "    Type: String\n"
                + "    Description: Specifies the AMI ID for your container instances.\n"
                + "  EcsInstanceType:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Specifies the EC2 instance type for your container instances.\n"
                + "      Defaults to m4.large\n"
                + "    Default: m4.large\n"
                + "    ConstraintDescription: must be a valid EC2 instance type.\n"
                + "  KeyName:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the name of an existing Amazon EC2 key pair\n"
                + "      to enable SSH access to the EC2 instances in your cluster.\n"
                + "    Default: ''\n"
                + "  VpcId:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the ID of an existing VPC in which to launch\n"
                + "      your container instances. If you specify a VPC ID, you must specify a list of\n"
                + "      existing subnets in that VPC. If you do not specify a VPC ID, a new VPC is created\n"
                + "      with atleast 1 subnet.\n"
                + "    Default: ''\n"
                + "    AllowedPattern: \"^(?:vpc-[0-9a-f]{8}|)$\"\n"
                + "    ConstraintDescription: >\n"
                + "      VPC Id must begin with 'vpc-' or leave blank to have a\n"
                + "      new VPC created\n"
                + "  SubnetIds:\n"
                + "    Type: CommaDelimitedList\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Comma separated list of existing VPC Subnet\n"
                + "      Ids where ECS instances will run\n"
                + "    Default: ''\n"
                + "  SecurityGroupId:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Security Group Id of an existing Security\n"
                + "      Group. Leave blank to have a new Security Group created\n"
                + "    Default: ''\n"
                + "  VpcCidr:\n"
                + "    Type: String\n"
                + "    Description: Optional - Specifies the CIDR Block of VPC\n"
                + "    Default: ''\n"
                + "  SubnetCidr1:\n"
                + "    Type: String\n"
                + "    Description: Specifies the CIDR Block of Subnet 1\n"
                + "    Default: ''\n"
                + "  SubnetCidr2:\n"
                + "    Type: String\n"
                + "    Description: Specifies the CIDR Block of Subnet 2\n"
                + "    Default: ''\n"
                + "  SubnetCidr3:\n"
                + "    Type: String\n"
                + "    Description: Specifies the CIDR Block of Subnet 3\n"
                + "    Default: ''\n"
                + "  AsgMaxSize:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Specifies the number of instances to launch and register to the cluster.\n"
                + "      Defaults to 1.\n"
                + "    Default: '1'\n"
                + "  IamRoleInstanceProfile:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Specifies the Name or the Amazon Resource Name (ARN) of the instance\n"
                + "      profile associated with the IAM role for the instance\n"
                + "  SecurityIngressFromPort:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Start of Security Group port to open on\n"
                + "      ECS instances - defaults to port 0\n"
                + "    Default: '0'\n"
                + "  SecurityIngressToPort:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Optional - Specifies the End of Security Group port to open on ECS\n"
                + "      instances - defaults to port 65535\n"
                + "    Default: '65535'\n"
                + "  SecurityIngressCidrIp:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the CIDR/IP range for Security Ports - defaults\n"
                + "      to 0.0.0.0/0\n"
                + "    Default: 0.0.0.0/0\n"
                + "  EcsEndpoint:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the ECS Endpoint for the ECS Agent to connect to\n"
                + "    Default: ''\n"
                + "  VpcAvailabilityZones:\n"
                + "    Type: CommaDelimitedList\n"
                + "    Description: >\n"
                + "      Specifies a comma-separated list of 3 VPC Availability Zones for\n"
                + "      the creation of new subnets. These zones must have the available status.\n"
                + "    Default: ''\n"
                + "  EbsVolumeSize:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Size in GBs, of the newly created Amazon\n"
                + "      Elastic Block Store (Amazon EBS) volume\n"
                + "    Default: '0'\n"
                + "  EbsVolumeType:\n"
                + "    Type: String\n"
                + "    Description: Optional - Specifies the Type of (Amazon EBS) volume\n"
                + "    Default: ''\n"
                + "    AllowedValues:\n"
                + "      - ''\n"
                + "      - standard\n"
                + "      - io1\n"
                + "      - gp2\n"
                + "      - sc1\n"
                + "      - st1\n"
                + "    ConstraintDescription: Must be a valid EC2 volume type.\n"
                + "  DeviceName:\n"
                + "    Type: String\n"
                + "    Description: Optional - Specifies the device mapping for the Volume\n"
                + "    Default: ''\n"
                + "Conditions:\n"
                + "  CreateEC2LCWithKeyPair:\n"
                + "    !Not [!Equals [!Ref KeyName, '']]\n"
                + "  SetEndpointToECSAgent:\n"
                + "    !Not [!Equals [!Ref EcsEndpoint, '']]\n"
                + "  CreateNewSecurityGroup:\n"
                + "    !Equals [!Ref SecurityGroupId, '']\n"
                + "  CreateNewVpc:\n"
                + "    !Equals [!Ref VpcId, '']\n"
                + "  CreateSubnet1: !And\n"
                + "    - !Not [!Equals [!Ref SubnetCidr1, '']]\n"
                + "    - !Condition CreateNewVpc\n"
                + "  CreateSubnet2: !And\n"
                + "    - !Not [!Equals [!Ref SubnetCidr2, '']]\n"
                + "    - !Condition CreateSubnet1\n"
                + "  CreateSubnet3: !And\n"
                + "    - !Not [!Equals [!Ref SubnetCidr3, '']]\n"
                + "    - !Condition CreateSubnet2\n"
                + "  CreateEbsVolume: !And\n"
                + "    - !Not [!Equals [!Ref EbsVolumeSize, '0']]\n"
                + "    - !Not [!Equals [!Ref EbsVolumeType, '']]\n"
                + "    - !Not [!Equals [!Ref DeviceName, '']]\n"
                + "Resources:\n"
                + "  Vpc:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::VPC\n"
                + "    Properties:\n"
                + "      CidrBlock: !Ref VpcCidr\n"
                + "      EnableDnsSupport: 'true'\n"
                + "      EnableDnsHostnames: 'true'\n"
                + "  PubSubnetAz1:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::Subnet\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      CidrBlock: !Ref SubnetCidr1\n"
                + "      AvailabilityZone: !Select [ 0, !Ref VpcAvailabilityZones ]\n"
                + "  PubSubnetAz2:\n"
                + "    Condition: CreateSubnet2\n"
                + "    Type: AWS::EC2::Subnet\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      CidrBlock: !Ref SubnetCidr2\n"
                + "      AvailabilityZone: !Select [ 1, !Ref VpcAvailabilityZones ]\n"
                + "  PubSubnetAz3:\n"
                + "    Condition: CreateSubnet3\n"
                + "    Type: AWS::EC2::Subnet\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      CidrBlock: !Ref SubnetCidr3\n"
                + "      AvailabilityZone: !Select [ 2, !Ref VpcAvailabilityZones ]\n"
                + "  InternetGateway:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::InternetGateway\n"
                + "  AttachGateway:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::VPCGatewayAttachment\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      InternetGatewayId: !Ref InternetGateway\n"
                + "  RouteViaIgw:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::RouteTable\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "  PublicRouteViaIgw:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::Route\n"
                + "    DependsOn: AttachGateway\n"
                + "    Properties:\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "      DestinationCidrBlock: 0.0.0.0/0\n"
                + "      GatewayId: !Ref InternetGateway\n"
                + "  PubSubnet1RouteTableAssociation:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::SubnetRouteTableAssociation\n"
                + "    Properties:\n"
                + "      SubnetId: !Ref PubSubnetAz1\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "  PubSubnet2RouteTableAssociation:\n"
                + "    Condition: CreateSubnet2\n"
                + "    Type: AWS::EC2::SubnetRouteTableAssociation\n"
                + "    Properties:\n"
                + "      SubnetId: !Ref PubSubnetAz2\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "  PubSubnet3RouteTableAssociation:\n"
                + "    Condition: CreateSubnet3\n"
                + "    Type: AWS::EC2::SubnetRouteTableAssociation\n"
                + "    Properties:\n"
                + "      SubnetId: !Ref PubSubnetAz3\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "  EcsSecurityGroup:\n"
                + "    Condition: CreateNewSecurityGroup\n"
                + "    Type: AWS::EC2::SecurityGroup\n"
                + "    Properties:\n"
                + "      GroupDescription: ECS Allowed Ports\n"
                + "      VpcId: !If [ CreateSubnet1, !Ref Vpc, !Ref VpcId ]\n"
                + "      SecurityGroupIngress:\n"
                + "        IpProtocol: tcp\n"
                + "        FromPort: !Ref SecurityIngressFromPort\n"
                + "        ToPort: !Ref SecurityIngressToPort\n"
                + "        CidrIp: !Ref SecurityIngressCidrIp\n"
                + "  EcsInstanceLc:\n"
                + "    Type: AWS::AutoScaling::LaunchConfiguration\n"
                + "    Properties:\n"
                + "      ImageId: !Ref EcsAmiId\n"
                + "      InstanceType: !Ref EcsInstanceType\n"
                + "      AssociatePublicIpAddress: true\n"
                + "      IamInstanceProfile: !Ref IamRoleInstanceProfile\n"
                + "      KeyName: !If [ CreateEC2LCWithKeyPair, !Ref KeyName, !Ref \"AWS::NoValue\" ]\n"
                + "      SecurityGroups: [ !If [ CreateNewSecurityGroup, !Ref EcsSecurityGroup, !Ref SecurityGroupId ] ]\n"
                + "      BlockDeviceMappings: !If\n"
                + "        - CreateEbsVolume\n"
                + "        -\n"
                + "          - DeviceName: !Ref DeviceName\n"
                + "            Ebs:\n"
                + "             VolumeSize: !Ref EbsVolumeSize\n"
                + "             VolumeType: !Ref EbsVolumeType\n"
                + "        - !Ref \"AWS::NoValue\"\n"
                + "      UserData: !If\n"
                + "        - SetEndpointToECSAgent\n"
                + "        - Fn::Base64: !Sub |\n"
                + "           #!/bin/bash\n"
                + "           echo ECS_CLUSTER=${EcsClusterName} >> /etc/ecs/ecs.config\n"
                + "           echo ECS_BACKEND_HOST=${EcsEndpoint} >> /etc/ecs/ecs.config\n"
                + "        - Fn::Base64: !Sub |\n"
                + "           #!/bin/bash\n"
                + "           echo ECS_CLUSTER=${EcsClusterName} >> /etc/ecs/ecs.config\n"
                + "  EcsInstanceAsg:\n"
                + "    Type: AWS::AutoScaling::AutoScalingGroup\n"
                + "    Properties:\n"
                + "      VPCZoneIdentifier: !If\n"
                + "        - CreateSubnet1\n"
                + "        - !If\n"
                + "          - CreateSubnet2\n"
                + "          - !If\n"
                + "            - CreateSubnet3\n"
                + "            - [ !Sub \"${PubSubnetAz1}, ${PubSubnetAz2}, ${PubSubnetAz3}\" ]\n"
                + "            - [ !Sub \"${PubSubnetAz1}, ${PubSubnetAz2}\" ]\n"
                + "          - [ !Sub \"${PubSubnetAz1}\" ]\n"
                + "        - !Ref SubnetIds\n"
                + "      LaunchConfigurationName: !Ref EcsInstanceLc\n"
                + "      MinSize: '0'\n"
                + "      MaxSize: !Ref AsgMaxSize\n"
                + "      DesiredCapacity: !Ref AsgMaxSize\n"
                + "      Tags:\n"
                + "        -\n"
                + "          Key: Name\n"
                + "          Value: !Sub \"ECS Instance - ${AWS::StackName}\"\n"
                + "          PropagateAtLaunch: 'true'\n"
                + "        -\n"
                + "          Key: Description\n"
                + "          Value: >\n"
                + "            This instance is the part of the Auto Scaling group which was created\n"
                + "            through ECS Console\n"
                + "          PropagateAtLaunch: 'true'\n"
                + "Outputs:\n"
                + "  EcsInstanceAsgName:\n"
                + "    Description: Auto Scaling Group Name for ECS Instances\n"
                + "    Value: !Ref EcsInstanceAsg\n"
                + "  UsedByECSCreateCluster:\n"
                + "    Description: Flag used by EC2 Container Service Create Cluster Wizard\n"
                + "    Value: 'true'")
            .withParameters(

                new Parameter().withParameterKey("AsgMaxSize").withParameterValue("1"),
                new Parameter().withParameterKey("DeviceName").withParameterValue("/dev/xvdcz"),
                new Parameter().withParameterKey("EbsVolumeSize").withParameterValue("22"),
                new Parameter().withParameterKey("EbsVolumeType").withParameterValue("gp2"),
                new Parameter().withParameterKey("EcsAmiId").withParameterValue("ami-d69c74c0"),
                new Parameter().withParameterKey("EcsClusterName").withParameterValue("demo"),
                new Parameter().withParameterKey("EcsInstanceType").withParameterValue("t2.micro"),
                new Parameter().withParameterKey("IamRoleInstanceProfile").withParameterValue("ecsInstanceRole"),
                new Parameter().withParameterKey("KeyName").withParameterValue("testkeypair"),
                new Parameter().withParameterKey("SecurityGroupId").withParameterValue("sg-eec03094"),
                new Parameter().withParameterKey("SecurityIngressCidrIp").withParameterValue("0.0.0.0/0"),
                new Parameter().withParameterKey("SecurityIngressFromPort").withParameterValue("80"),
                new Parameter().withParameterKey("SecurityIngressToPort").withParameterValue("80"),
                new Parameter().withParameterKey("SubnetCidr1").withParameterValue("10.0.0.0/24"),
                new Parameter().withParameterKey("SubnetCidr2").withParameterValue("10.0.0.0/24"),
                new Parameter().withParameterKey("SubnetCidr3").withParameterValue("10.0.0.0/24"),
                new Parameter()
                    .withParameterKey("SubnetIds")
                    .withParameterValue("subnet-9725a6bd,subnet-42ddaf34,subnet-64d99b59,subnet-fbe268a3"),
                new Parameter()
                    .withParameterKey("VpcAvailabilityZones")
                    .withParameterValue("us-east-1e,us-east-1c,us-east-1d,us-east-1a"),
                new Parameter().withParameterKey("VpcCidr").withParameterValue("10.0.0.0/16"),
                new Parameter().withParameterKey("VpcId").withParameterValue("vpc-84a9bfe0")),
        AwsConfig.builder()
            .accessKey("AKIAJLEKM45P4PO5QUFQ")
            .secretKey("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".toCharArray())
            .build());

    Stack stack;
    while (!"CREATE_COMPLETE".equals((
        stack = awsHelperService
                    .describeStacks("us-east-1", new DescribeStacksRequest().withStackName("EC2ContainerService-test2"),
                        AwsConfig.builder()
                            .accessKey("AKIAJLEKM45P4PO5QUFQ")
                            .secretKey("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".toCharArray())
                            .build())
                    .getStacks()
                    .get(0))
                                         .getStackStatus())) {
      sleep(ofSeconds(1));
    }

    stack.getOutputs().forEach(output -> logger.info(output.getOutputKey() + " = " + output.getOutputValue()));
  }

  /**
   * Destroy cluster.
   */
  public void destroyCluster() {
    awsHelperService.createStack("us-east-1",
        new CreateStackRequest()
            .withStackName("EC2ContainerService-test2")
            .withTemplateBody("AWSTemplateFormatVersion: '2010-09-09'\n"
                + "Description: >\n"
                + "  AWS CloudFormation template to create a new VPC\n"
                + "  or use an existing VPC for ECS deployment\n"
                + "  in Create Cluster Wizard\n"
                + "Parameters:\n"
                + "  EcsClusterName:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Specifies the ECS Cluster Name with which the resources would be\n"
                + "      associated\n"
                + "    Default: default\n"
                + "  EcsAmiId:\n"
                + "    Type: String\n"
                + "    Description: Specifies the AMI ID for your container instances.\n"
                + "  EcsInstanceType:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Specifies the EC2 instance type for your container instances.\n"
                + "      Defaults to m4.large\n"
                + "    Default: m4.large\n"
                + "    ConstraintDescription: must be a valid EC2 instance type.\n"
                + "  KeyName:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the name of an existing Amazon EC2 key pair\n"
                + "      to enable SSH access to the EC2 instances in your cluster.\n"
                + "    Default: ''\n"
                + "  VpcId:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the ID of an existing VPC in which to launch\n"
                + "      your container instances. If you specify a VPC ID, you must specify a list of\n"
                + "      existing subnets in that VPC. If you do not specify a VPC ID, a new VPC is created\n"
                + "      with atleast 1 subnet.\n"
                + "    Default: ''\n"
                + "    AllowedPattern: \"^(?:vpc-[0-9a-f]{8}|)$\"\n"
                + "    ConstraintDescription: >\n"
                + "      VPC Id must begin with 'vpc-' or leave blank to have a\n"
                + "      new VPC created\n"
                + "  SubnetIds:\n"
                + "    Type: CommaDelimitedList\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Comma separated list of existing VPC Subnet\n"
                + "      Ids where ECS instances will run\n"
                + "    Default: ''\n"
                + "  SecurityGroupId:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Security Group Id of an existing Security\n"
                + "      Group. Leave blank to have a new Security Group created\n"
                + "    Default: ''\n"
                + "  VpcCidr:\n"
                + "    Type: String\n"
                + "    Description: Optional - Specifies the CIDR Block of VPC\n"
                + "    Default: ''\n"
                + "  SubnetCidr1:\n"
                + "    Type: String\n"
                + "    Description: Specifies the CIDR Block of Subnet 1\n"
                + "    Default: ''\n"
                + "  SubnetCidr2:\n"
                + "    Type: String\n"
                + "    Description: Specifies the CIDR Block of Subnet 2\n"
                + "    Default: ''\n"
                + "  SubnetCidr3:\n"
                + "    Type: String\n"
                + "    Description: Specifies the CIDR Block of Subnet 3\n"
                + "    Default: ''\n"
                + "  AsgMaxSize:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Specifies the number of instances to launch and register to the cluster.\n"
                + "      Defaults to 1.\n"
                + "    Default: '1'\n"
                + "  IamRoleInstanceProfile:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Specifies the Name or the Amazon Resource Name (ARN) of the instance\n"
                + "      profile associated with the IAM role for the instance\n"
                + "  SecurityIngressFromPort:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Start of Security Group port to open on\n"
                + "      ECS instances - defaults to port 0\n"
                + "    Default: '0'\n"
                + "  SecurityIngressToPort:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Optional - Specifies the End of Security Group port to open on ECS\n"
                + "      instances - defaults to port 65535\n"
                + "    Default: '65535'\n"
                + "  SecurityIngressCidrIp:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the CIDR/IP range for Security Ports - defaults\n"
                + "      to 0.0.0.0/0\n"
                + "    Default: 0.0.0.0/0\n"
                + "  EcsEndpoint:\n"
                + "    Type: String\n"
                + "    Description: >\n"
                + "      Optional - Specifies the ECS Endpoint for the ECS Agent to connect to\n"
                + "    Default: ''\n"
                + "  VpcAvailabilityZones:\n"
                + "    Type: CommaDelimitedList\n"
                + "    Description: >\n"
                + "      Specifies a comma-separated list of 3 VPC Availability Zones for\n"
                + "      the creation of new subnets. These zones must have the available status.\n"
                + "    Default: ''\n"
                + "  EbsVolumeSize:\n"
                + "    Type: Number\n"
                + "    Description: >\n"
                + "      Optional - Specifies the Size in GBs, of the newly created Amazon\n"
                + "      Elastic Block Store (Amazon EBS) volume\n"
                + "    Default: '0'\n"
                + "  EbsVolumeType:\n"
                + "    Type: String\n"
                + "    Description: Optional - Specifies the Type of (Amazon EBS) volume\n"
                + "    Default: ''\n"
                + "    AllowedValues:\n"
                + "      - ''\n"
                + "      - standard\n"
                + "      - io1\n"
                + "      - gp2\n"
                + "      - sc1\n"
                + "      - st1\n"
                + "    ConstraintDescription: Must be a valid EC2 volume type.\n"
                + "  DeviceName:\n"
                + "    Type: String\n"
                + "    Description: Optional - Specifies the device mapping for the Volume\n"
                + "    Default: ''\n"
                + "Conditions:\n"
                + "  CreateEC2LCWithKeyPair:\n"
                + "    !Not [!Equals [!Ref KeyName, '']]\n"
                + "  SetEndpointToECSAgent:\n"
                + "    !Not [!Equals [!Ref EcsEndpoint, '']]\n"
                + "  CreateNewSecurityGroup:\n"
                + "    !Equals [!Ref SecurityGroupId, '']\n"
                + "  CreateNewVpc:\n"
                + "    !Equals [!Ref VpcId, '']\n"
                + "  CreateSubnet1: !And\n"
                + "    - !Not [!Equals [!Ref SubnetCidr1, '']]\n"
                + "    - !Condition CreateNewVpc\n"
                + "  CreateSubnet2: !And\n"
                + "    - !Not [!Equals [!Ref SubnetCidr2, '']]\n"
                + "    - !Condition CreateSubnet1\n"
                + "  CreateSubnet3: !And\n"
                + "    - !Not [!Equals [!Ref SubnetCidr3, '']]\n"
                + "    - !Condition CreateSubnet2\n"
                + "  CreateEbsVolume: !And\n"
                + "    - !Not [!Equals [!Ref EbsVolumeSize, '0']]\n"
                + "    - !Not [!Equals [!Ref EbsVolumeType, '']]\n"
                + "    - !Not [!Equals [!Ref DeviceName, '']]\n"
                + "Resources:\n"
                + "  Vpc:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::VPC\n"
                + "    Properties:\n"
                + "      CidrBlock: !Ref VpcCidr\n"
                + "      EnableDnsSupport: 'true'\n"
                + "      EnableDnsHostnames: 'true'\n"
                + "  PubSubnetAz1:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::Subnet\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      CidrBlock: !Ref SubnetCidr1\n"
                + "      AvailabilityZone: !Select [ 0, !Ref VpcAvailabilityZones ]\n"
                + "  PubSubnetAz2:\n"
                + "    Condition: CreateSubnet2\n"
                + "    Type: AWS::EC2::Subnet\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      CidrBlock: !Ref SubnetCidr2\n"
                + "      AvailabilityZone: !Select [ 1, !Ref VpcAvailabilityZones ]\n"
                + "  PubSubnetAz3:\n"
                + "    Condition: CreateSubnet3\n"
                + "    Type: AWS::EC2::Subnet\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      CidrBlock: !Ref SubnetCidr3\n"
                + "      AvailabilityZone: !Select [ 2, !Ref VpcAvailabilityZones ]\n"
                + "  InternetGateway:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::InternetGateway\n"
                + "  AttachGateway:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::VPCGatewayAttachment\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "      InternetGatewayId: !Ref InternetGateway\n"
                + "  RouteViaIgw:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::RouteTable\n"
                + "    Properties:\n"
                + "      VpcId: !Ref Vpc\n"
                + "  PublicRouteViaIgw:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::Route\n"
                + "    DependsOn: AttachGateway\n"
                + "    Properties:\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "      DestinationCidrBlock: 0.0.0.0/0\n"
                + "      GatewayId: !Ref InternetGateway\n"
                + "  PubSubnet1RouteTableAssociation:\n"
                + "    Condition: CreateSubnet1\n"
                + "    Type: AWS::EC2::SubnetRouteTableAssociation\n"
                + "    Properties:\n"
                + "      SubnetId: !Ref PubSubnetAz1\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "  PubSubnet2RouteTableAssociation:\n"
                + "    Condition: CreateSubnet2\n"
                + "    Type: AWS::EC2::SubnetRouteTableAssociation\n"
                + "    Properties:\n"
                + "      SubnetId: !Ref PubSubnetAz2\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "  PubSubnet3RouteTableAssociation:\n"
                + "    Condition: CreateSubnet3\n"
                + "    Type: AWS::EC2::SubnetRouteTableAssociation\n"
                + "    Properties:\n"
                + "      SubnetId: !Ref PubSubnetAz3\n"
                + "      RouteTableId: !Ref RouteViaIgw\n"
                + "  EcsSecurityGroup:\n"
                + "    Condition: CreateNewSecurityGroup\n"
                + "    Type: AWS::EC2::SecurityGroup\n"
                + "    Properties:\n"
                + "      GroupDescription: ECS Allowed Ports\n"
                + "      VpcId: !If [ CreateSubnet1, !Ref Vpc, !Ref VpcId ]\n"
                + "      SecurityGroupIngress:\n"
                + "        IpProtocol: tcp\n"
                + "        FromPort: !Ref SecurityIngressFromPort\n"
                + "        ToPort: !Ref SecurityIngressToPort\n"
                + "        CidrIp: !Ref SecurityIngressCidrIp\n"
                + "  EcsInstanceLc:\n"
                + "    Type: AWS::AutoScaling::LaunchConfiguration\n"
                + "    Properties:\n"
                + "      ImageId: !Ref EcsAmiId\n"
                + "      InstanceType: !Ref EcsInstanceType\n"
                + "      AssociatePublicIpAddress: true\n"
                + "      IamInstanceProfile: !Ref IamRoleInstanceProfile\n"
                + "      KeyName: !If [ CreateEC2LCWithKeyPair, !Ref KeyName, !Ref \"AWS::NoValue\" ]\n"
                + "      SecurityGroups: [ !If [ CreateNewSecurityGroup, !Ref EcsSecurityGroup, !Ref SecurityGroupId ] ]\n"
                + "      BlockDeviceMappings: !If\n"
                + "        - CreateEbsVolume\n"
                + "        -\n"
                + "          - DeviceName: !Ref DeviceName\n"
                + "            Ebs:\n"
                + "             VolumeSize: !Ref EbsVolumeSize\n"
                + "             VolumeType: !Ref EbsVolumeType\n"
                + "        - !Ref \"AWS::NoValue\"\n"
                + "      UserData: !If\n"
                + "        - SetEndpointToECSAgent\n"
                + "        - Fn::Base64: !Sub |\n"
                + "           #!/bin/bash\n"
                + "           echo ECS_CLUSTER=${EcsClusterName} >> /etc/ecs/ecs.config\n"
                + "           echo ECS_BACKEND_HOST=${EcsEndpoint} >> /etc/ecs/ecs.config\n"
                + "        - Fn::Base64: !Sub |\n"
                + "           #!/bin/bash\n"
                + "           echo ECS_CLUSTER=${EcsClusterName} >> /etc/ecs/ecs.config\n"
                + "  EcsInstanceAsg:\n"
                + "    Type: AWS::AutoScaling::AutoScalingGroup\n"
                + "    Properties:\n"
                + "      VPCZoneIdentifier: !If\n"
                + "        - CreateSubnet1\n"
                + "        - !If\n"
                + "          - CreateSubnet2\n"
                + "          - !If\n"
                + "            - CreateSubnet3\n"
                + "            - [ !Sub \"${PubSubnetAz1}, ${PubSubnetAz2}, ${PubSubnetAz3}\" ]\n"
                + "            - [ !Sub \"${PubSubnetAz1}, ${PubSubnetAz2}\" ]\n"
                + "          - [ !Sub \"${PubSubnetAz1}\" ]\n"
                + "        - !Ref SubnetIds\n"
                + "      LaunchConfigurationName: !Ref EcsInstanceLc\n"
                + "      MinSize: '0'\n"
                + "      MaxSize: !Ref AsgMaxSize\n"
                + "      DesiredCapacity: !Ref AsgMaxSize\n"
                + "      Tags:\n"
                + "        -\n"
                + "          Key: Name\n"
                + "          Value: !Sub \"ECS Instance - ${AWS::StackName}\"\n"
                + "          PropagateAtLaunch: 'true'\n"
                + "        -\n"
                + "          Key: Description\n"
                + "          Value: >\n"
                + "            This instance is the part of the Auto Scaling group which was created\n"
                + "            through ECS Console\n"
                + "          PropagateAtLaunch: 'true'\n"
                + "Outputs:\n"
                + "  EcsInstanceAsgName:\n"
                + "    Description: Auto Scaling Group Name for ECS Instances\n"
                + "    Value: !Ref EcsInstanceAsg\n"
                + "  UsedByECSCreateCluster:\n"
                + "    Description: Flag used by EC2 Container Service Create Cluster Wizard\n"
                + "    Value: 'true'")
            .withParameters(

                new Parameter().withParameterKey("AsgMaxSize").withParameterValue("1"),
                new Parameter().withParameterKey("DeviceName").withParameterValue("/dev/xvdcz"),
                new Parameter().withParameterKey("EbsVolumeSize").withParameterValue("22"),
                new Parameter().withParameterKey("EbsVolumeType").withParameterValue("gp2"),
                new Parameter().withParameterKey("EcsAmiId").withParameterValue("ami-d69c74c0"),
                new Parameter().withParameterKey("EcsClusterName").withParameterValue("test2"),
                new Parameter().withParameterKey("EcsInstanceType").withParameterValue("t2.micro"),
                new Parameter().withParameterKey("IamRoleInstanceProfile").withParameterValue("ecsInstanceRole"),
                new Parameter().withParameterKey("KeyName").withParameterValue("testkeypair"),
                new Parameter().withParameterKey("SecurityGroupId").withParameterValue("sg-eec03094"),
                new Parameter().withParameterKey("SecurityIngressCidrIp").withParameterValue("0.0.0.0/0"),
                new Parameter().withParameterKey("SecurityIngressFromPort").withParameterValue("80"),
                new Parameter().withParameterKey("SecurityIngressToPort").withParameterValue("80"),
                new Parameter().withParameterKey("SubnetCidr1").withParameterValue("10.0.0.0/24"),
                new Parameter().withParameterKey("SubnetCidr2").withParameterValue("10.0.0.0/24"),
                new Parameter().withParameterKey("SubnetCidr3").withParameterValue("10.0.0.0/24"),
                new Parameter()
                    .withParameterKey("SubnetIds")
                    .withParameterValue("subnet-9725a6bd,subnet-42ddaf34,subnet-64d99b59,subnet-fbe268a3"),
                new Parameter()
                    .withParameterKey("VpcAvailabilityZones")
                    .withParameterValue("us-east-1e,us-east-1c,us-east-1d,us-east-1a"),
                new Parameter().withParameterKey("VpcCidr").withParameterValue("10.0.0.0/16"),
                new Parameter().withParameterKey("VpcId").withParameterValue("vpc-84a9bfe0")),
        AwsConfig.builder()
            .accessKey("AKIAJLEKM45P4PO5QUFQ")
            .secretKey("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".toCharArray())
            .build());

    Stack stack;
    while (!"CREATE_COMPLETE".equals((
        stack = awsHelperService
                    .describeStacks("us-east-1", new DescribeStacksRequest().withStackName("EC2ContainerService-test2"),
                        AwsConfig.builder()
                            .accessKey("AKIAJLEKM45P4PO5QUFQ")
                            .secretKey("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".toCharArray())
                            .build())
                    .getStacks()
                    .get(0))
                                         .getStackStatus())) {
      sleep(ofSeconds(1));
    }

    stack.getOutputs().forEach(output -> logger.info(output.getOutputKey() + " = " + output.getOutputValue()));
  }

  @Override
  public void provisionNodes(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, Integer clusterSize, String launchConfigName,
      Map<String, Object> params, LogCallback logCallback) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);

    String clusterName = (String) params.get("clusterName");
    awsHelperService.createCluster(
        region, awsConfig, encryptedDataDetails, new CreateClusterRequest().withClusterName(clusterName));
    logger.info("Successfully created empty cluster " + params.get("clusterName"));

    logger.info("Creating autoscaling group for cluster...");

    Integer maxSize = (Integer) params.computeIfAbsent("maxSize", s -> 2 * clusterSize); // default 200%
    Integer minSize = (Integer) params.computeIfAbsent("minSize", s -> clusterSize / 2); // default 50%
    String autoScalingGroupName = (String) params.get("autoScalingGroupName");
    String vpcZoneIdentifiers = (String) params.get("vpcZoneIdentifiers");
    List<String> availabilityZones = (List<String>) params.get("availabilityZones");

    logger.info("Creating autoscaling group for cluster...");
    awsHelperService.createAutoScalingGroup(awsConfig, encryptedDataDetails, region,
        new CreateAutoScalingGroupRequest()
            .withLaunchConfigurationName(launchConfigName)
            .withDesiredCapacity(clusterSize)
            .withMaxSize(maxSize)
            .withMinSize(minSize)
            .withAutoScalingGroupName(autoScalingGroupName)
            .withAvailabilityZones(availabilityZones)
            .withVPCZoneIdentifier(vpcZoneIdentifiers),
        logCallback);

    logger.info("Successfully created autoScalingGroup: {}", autoScalingGroupName);

    waitForAllInstancesToBeReady(awsConfig, encryptedDataDetails, region, autoScalingGroupName, clusterSize);
    waitForAllInstanceToRegisterWithCluster(region, awsConfig, encryptedDataDetails, clusterName, clusterSize);

    logger.info("All instances are ready for deployment");
  }

  private void waitForAllInstanceToRegisterWithCluster(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, Integer clusterSize) {
    try {
      timeLimiter.callWithTimeout(() -> {
        while (!allInstancesRegisteredWithCluster(region, awsConfig, encryptedDataDetails, clusterName, clusterSize)) {
          sleep(ofSeconds(10));
        }
        return true;
      }, 10L, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      throw new WingsException(INIT_TIMEOUT)
          .addParam("message", "Timed out waiting for instances to register with cluster");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for instances to register with cluster", e);
    }
  }

  private void waitForAllInstancesToBeReady(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String autoscalingGroupName, Integer clusterSize) {
    try {
      timeLimiter.callWithTimeout(() -> {
        while (!allInstanceInReadyState(awsConfig, encryptedDataDetails, region, autoscalingGroupName, clusterSize)) {
          sleep(ofSeconds(10));
        }
        return true;
      }, 10L, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      throw new WingsException(INIT_TIMEOUT).addParam("message", "Timed out waiting for instances to be ready");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for instances to be ready", e);
    }
  }

  private boolean allInstancesRegisteredWithCluster(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String name, Integer clusterSize) {
    Cluster cluster =
        awsHelperService
            .describeClusters(region, awsConfig, encryptedDataDetails, new DescribeClustersRequest().withClusters(name))
            .getClusters()
            .get(0);
    logger.info("Waiting for instances to register with cluster. {}/{} registered...",
        cluster.getRegisteredContainerInstancesCount(), clusterSize);

    return cluster.getRegisteredContainerInstancesCount().equals(clusterSize);
  }

  private boolean allInstanceInReadyState(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String name, Integer clusterSize) {
    AutoScalingGroup autoScalingGroup =
        awsHelperService
            .describeAutoScalingGroups(awsConfig, encryptedDataDetails, region,
                new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asList(name)))
            .getAutoScalingGroups()
            .get(0);
    List<Instance> instances = autoScalingGroup.getInstances();
    logger.info("Waiting for all instances to be ready. {}/{} ready...", instances.size(), clusterSize);
    return !instances.isEmpty()
        && instances.stream().allMatch(instance -> "InService".equals(instance.getLifecycleState()));
  }

  @Override
  public String deployService(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String serviceDefinition) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);
    CreateServiceRequest createServiceRequest;
    try {
      createServiceRequest = mapper.readValue(serviceDefinition, CreateServiceRequest.class);
    } catch (IOException ex) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(ex), ex);
    }
    logger.info("Begin service deployment " + createServiceRequest.getServiceName());
    CreateServiceResult createServiceResult =
        awsHelperService.createService(region, awsConfig, encryptedDataDetails, createServiceRequest);

    List<ServiceEvent> serviceEvents = getServiceEvents(region, awsConfig, encryptedDataDetails,
        createServiceRequest.getCluster(), createServiceRequest.getServiceName());

    waitForTasksToBeInRunningState(UpdateServiceCountRequestData.builder()
                                       .awsConfig(awsConfig)
                                       .region(region)
                                       .cluster(createServiceRequest.getCluster())
                                       .encryptedDataDetails(encryptedDataDetails)
                                       .serviceName(createServiceRequest.getServiceName())
                                       .executionLogCallback(new ExecutionLogCallback())
                                       .desiredCount(createServiceRequest.getDesiredCount())
                                       .serviceEvents(serviceEvents)
                                       .build());

    return createServiceResult.getService().getServiceArn();
  }

  private void waitForTasksToBeInRunningState(UpdateServiceCountRequestData requestData) {
    try {
      timeLimiter.callWithTimeout(() -> {
        while (notAllDesiredTasksRunning(requestData)) {
          sleep(ofSeconds(10));
        }
        return true;
      }, 10L, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      throw new TimeoutException(
          "Timed out waiting for tasks to be in running state", "Timeout", e, WingsException.SRE);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for tasks to be in running state", e);
    }
  }

  @Override
  public void waitForTasksToBeInRunningStateButDontThrowException(UpdateServiceCountRequestData requestData) {
    try {
      waitForTasksToBeInRunningState(requestData);
    } catch (TimeoutException e) {
      throw e;
    } catch (WingsException e) {
      if (e.getCode() == INIT_TIMEOUT) {
        throw e;
      }
    }
  }

  private boolean notAllDesiredTasksRunning(UpdateServiceCountRequestData requestData) {
    ExecutionLogCallback executionLogCallback = requestData.getExecutionLogCallback();
    Service service = getEcsServicesForCluster(requestData.getRegion(), requestData.getAwsConfig(),
        requestData.getEncryptedDataDetails(), requestData.getCluster(), Arrays.asList(requestData.getServiceName()))
                          .get(0);

    logger.info(
        "Waiting for pending tasks to finish. {}/{} running ...", service.getRunningCount(), service.getDesiredCount());

    executionLogCallback.saveExecutionLog(format("Waiting for pending tasks to finish. %s/%s running ...",
                                              service.getRunningCount(), service.getDesiredCount()),
        LogLevel.INFO);

    printAwsEvent(service, requestData.getServiceEvents(), executionLogCallback);
    return !Integer.valueOf(requestData.getDesiredCount()).equals(service.getRunningCount());
  }

  @Override
  public void deleteService(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);
    awsHelperService.deleteService(region, awsConfig, encryptedDataDetails,
        new DeleteServiceRequest().withCluster(clusterName).withService(serviceName));
  }

  @Override
  public List<ContainerInfo> waitForDaemonServiceToReachSteadyState(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);

    Service service =
        getEcsServicesForCluster(region, awsConfig, encryptedDataDetails, clusterName, Arrays.asList(serviceName))
            .get(0);

    UpdateServiceCountRequestData serviceCountRequestData = UpdateServiceCountRequestData.builder()
                                                                .encryptedDataDetails(encryptedDataDetails)
                                                                .region(region)
                                                                .serviceName(serviceName)
                                                                .cluster(clusterName)
                                                                .desiredCount(service.getDesiredCount())
                                                                .executionLogCallback(executionLogCallback)
                                                                .awsConfig(awsConfig)
                                                                .serviceEvents(getEventsFromService(service))
                                                                .build();

    waitForTasksToBeInRunningStateButDontThrowException(serviceCountRequestData);
    waitForServiceToReachSteadyState(serviceSteadyStateTimeout, serviceCountRequestData);
    return getContainerInfosAfterEcsWait(region, awsConfig, encryptedDataDetails, clusterName, serviceName,
        Collections.EMPTY_LIST, executionLogCallback);
  }

  @Override
  public List<ContainerInfo> provisionTasks(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName, int previousCount,
      int desiredCount, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);

    try {
      List<String> originalTaskArns =
          getTaskArns(region, encryptedDataDetails, clusterName, serviceName, awsConfig, DesiredStatus.RUNNING);
      Service service =
          getEcsServicesForCluster(region, awsConfig, encryptedDataDetails, clusterName, Arrays.asList(serviceName))
              .get(0);

      // If service task count is already equal to desired count, don't try to resize
      if (service.getDesiredCount().intValue() != desiredCount) {
        List<ServiceEvent> serviceEvents = new ArrayList<>();
        if (isNotEmpty(service.getEvents())) {
          serviceEvents.addAll(service.getEvents());
        }

        UpdateServiceCountRequestData serviceCountRequestData = UpdateServiceCountRequestData.builder()
                                                                    .region(region)
                                                                    .encryptedDataDetails(encryptedDataDetails)
                                                                    .cluster(clusterName)
                                                                    .serviceName(serviceName)
                                                                    .desiredCount(desiredCount)
                                                                    .executionLogCallback(executionLogCallback)
                                                                    .awsConfig(awsConfig)
                                                                    .serviceEvents(serviceEvents)
                                                                    .build();

        updateServiceCount(serviceCountRequestData);
        executionLogCallback.saveExecutionLog("Service update request successfully submitted.", LogLevel.INFO);
        waitForTasksToBeInRunningStateButDontThrowException(serviceCountRequestData);
        if (desiredCount > previousCount) { // don't do it for downsize.
          waitForServiceToReachSteadyState(serviceSteadyStateTimeout, serviceCountRequestData);
        }
      }

      return getContainerInfosAfterEcsWait(
          region, awsConfig, encryptedDataDetails, clusterName, serviceName, originalTaskArns, executionLogCallback);
    } catch (TimeoutException e) {
      throw e;
    } catch (Exception ex) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(ex), ex);
    }
  }

  @Override
  public void updateServiceCount(UpdateServiceCountRequestData updateServiceCountRequestData) {
    UpdateServiceRequest updateServiceRequest = new UpdateServiceRequest()
                                                    .withCluster(updateServiceCountRequestData.getCluster())
                                                    .withService(updateServiceCountRequestData.getServiceName())
                                                    .withDesiredCount(updateServiceCountRequestData.getDesiredCount());

    UpdateServiceResult updateServiceResult = awsHelperService.updateService(updateServiceCountRequestData.getRegion(),
        updateServiceCountRequestData.getAwsConfig(), updateServiceCountRequestData.getEncryptedDataDetails(),
        updateServiceRequest);

    updateServiceCountRequestData.setServiceEvents(getEventsFromService(updateServiceResult.getService()));

    waitForServiceUpdateToComplete(updateServiceResult, updateServiceCountRequestData);
  }

  @Override
  public List<ContainerInfo> getContainerInfosAfterEcsWait(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      List<String> originalTaskArns, ExecutionLogCallback executionLogCallback) {
    List<String> taskArns =
        getTaskArns(region, encryptedDataDetails, clusterName, serviceName, awsConfig, DesiredStatus.RUNNING);
    if (isEmpty(taskArns)) {
      logger.info("Downsize complete for ECS deployment, Service: " + serviceName);
      return emptyList();
    }

    logger.info("Task arns = " + taskArns);
    List<Task> tasks = awsHelperService
                           .describeTasks(region, awsConfig, encryptedDataDetails,
                               new DescribeTasksRequest().withCluster(clusterName).withTasks(taskArns))
                           .getTasks();

    List<ContainerInfo> containerInfos = null;
    if (CollectionUtils.isNotEmpty(tasks)) {
      containerInfos = generateContainerInfos(tasks, clusterName, region, encryptedDataDetails, executionLogCallback,
          awsConfig, taskArns, originalTaskArns);
    } else {
      logger.warn("Could not fetched tasks, aws.describeTasks returned 0 tasks");
    }

    logger.info("Docker container ids = " + containerInfos);
    return containerInfos;
  }

  String getIdFromArn(String arn) {
    if (isNotEmpty(arn)) {
      String[] tokens = arn.split("/");
      return tokens[tokens.length - 1];
    }
    return StringUtils.EMPTY;
  }

  @VisibleForTesting
  Container getMainHarnessDeployedContainer(
      Task task, String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    Container mainContainer = null;
    if (isEmpty(task.getContainers())) {
      return mainContainer;
    }

    mainContainer = task.getContainers().get(0);

    if (task.getContainers().size() > 1) {
      DescribeTaskDefinitionRequest describeTaskDefinitionRequest = new DescribeTaskDefinitionRequest()
                                                                        .withTaskDefinition(task.getTaskDefinitionArn())
                                                                        .withInclude(TaskDefinitionField.TAGS);
      DescribeTaskDefinitionResult taskDefinitionResult = awsHelperService.describeTaskDefinition(
          region, awsConfig, encryptedDataDetails, describeTaskDefinitionRequest);
      List<Tag> taskDefinitionTags = taskDefinitionResult.getTags();

      if (isNotEmpty(taskDefinitionTags)) {
        Optional<Tag> tag = taskDefinitionTags.stream()
                                .filter(currentTag -> MAIN_ECS_CONTAINER_NAME_TAG.equals(currentTag.getKey()))
                                .findFirst();

        if (tag.isPresent()) {
          Optional<Container> mainContainerOptional =
              task.getContainers()
                  .stream()
                  .filter(container -> container.getName().equals(tag.get().getValue()))
                  .findFirst();
          if (mainContainerOptional.isPresent()) {
            mainContainer = mainContainerOptional.get();
          }
        }
      }
    }

    return mainContainer;
  }

  @Override
  public List<ContainerInfo> generateContainerInfos(List<Task> tasks, String clusterName, String region,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback, AwsConfig awsConfig,
      List<String> taskArns, List<String> originalTaskArns) {
    List<ContainerInfo> containerInfos = new ArrayList<>();

    List<Task> ec2Tasks =
        tasks.stream().filter(task -> !LaunchType.FARGATE.name().equals(task.getLaunchType())).collect(toList());

    List<Task> fargateTasks =
        tasks.stream().filter(task -> LaunchType.FARGATE.name().equals(task.getLaunchType())).collect(toList());

    // Handle fargate tasks
    if (CollectionUtils.isNotEmpty(fargateTasks)) {
      logger.warn("For Fargate tasks, AWS does not expose Container instances and EC2 instances, "
          + "so those details will not be available");
      for (Task fargateTask : tasks) {
        Container mainContainer = getMainHarnessDeployedContainer(fargateTask, region, awsConfig, encryptedDataDetails);
        EcsContainerDetailsBuilder ecsContainerDetailsBuilder =
            getEcsContainerDetailsBuilder(fargateTask, mainContainer);

        String privateIpv4AddressForENI = StringUtils.EMPTY;
        if (mainContainer != null) {
          privateIpv4AddressForENI = mainContainer.getNetworkInterfaces()
                                         .stream()
                                         .findFirst()
                                         .map(NetworkInterface::getPrivateIpv4Address)
                                         .orElse(StringUtils.EMPTY);
        }

        String dockerId =
            mainContainer == null ? StringUtils.EMPTY : StringUtils.substring(mainContainer.getRuntimeId(), 0, 12);
        ContainerInfo containerInfo = ContainerInfo.builder()
                                          .status(Status.SUCCESS)
                                          .containerId(dockerId)
                                          .hostName(dockerId)
                                          .ecsContainerDetails(ecsContainerDetailsBuilder.build())
                                          .ip(privateIpv4AddressForENI)
                                          .newContainer(!originalTaskArns.contains(fargateTask.getTaskArn()))
                                          .containerTasksReachable(mainContainer != null)
                                          .build();
        containerInfos.add(containerInfo);
      }
    }

    // Handle EC2 tasks
    if (CollectionUtils.isNotEmpty(ec2Tasks)) {
      Map<String, Task> containerTaskArns = new HashMap<>();
      for (Task task : tasks) {
        containerTaskArns.put(task.getContainerInstanceArn(), task);
      }
      List<ContainerInstance> containerInstanceList =
          fetchContainerInstancesForTasks(tasks, clusterName, region, encryptedDataDetails, awsConfig);

      com.amazonaws.services.ec2.model.Instance ec2Instance = null;
      for (ContainerInstance containerInstance : containerInstanceList) {
        String containerInstanceArn = containerInstance.getContainerInstanceArn();
        Task taskForContainerInstance = containerTaskArns.get(containerInstanceArn);
        if (taskForContainerInstance == null) {
          continue;
        }

        // ContainerInfo.ecsContainerDetails
        Container mainContainer =
            getMainHarnessDeployedContainer(taskForContainerInstance, region, awsConfig, encryptedDataDetails);
        EcsContainerDetailsBuilder ecsContainerDetailsBuilder =
            initEcsContainerDetailsBuilder(taskForContainerInstance, mainContainer);

        String dockerId =
            mainContainer == null ? StringUtils.EMPTY : StringUtils.substring(mainContainer.getRuntimeId(), 0, 12);
        ec2Instance = awsHelperService
                          .describeEc2Instances(awsConfig, encryptedDataDetails, region,
                              new DescribeInstancesRequest().withInstanceIds(containerInstance.getEc2InstanceId()))
                          .getReservations()
                          .get(0)
                          .getInstances()
                          .get(0);

        String ipAddress = ec2Instance.getPrivateIpAddress();
        if (dockerId == null) {
          // Try metadata api to get dockerId
          dockerId = awsMetadataApiHelper.tryMetadataApiForDockerIdIfAccessble(
              ec2Instance, taskForContainerInstance, mainContainer.getName(), executionLogCallback);
          if (isNotEmpty(dockerId)) {
            ecsContainerDetailsBuilder.completeDockerId(dockerId);
            dockerId = StringUtils.substring(dockerId, 0, 12);
            ecsContainerDetailsBuilder.dockerId(dockerId);
          }
        }

        // Instance will always have privateIp, but if is null for any reason, this is safeguard not to have NPE
        if (ipAddress == null) {
          logger.error(
              "ECS Deployment ALERT:- Ec2Instance is not expected to have NULL PrivateIp, something seems wrong");
          ipAddress = StringUtils.EMPTY;
        }

        if (mainContainer != null && isNotEmpty(mainContainer.getNetworkInterfaces())) {
          String privateIpv4AddressForENI = mainContainer.getNetworkInterfaces()
                                                .stream()
                                                .findFirst()
                                                .map(NetworkInterface::getPrivateIpv4Address)
                                                .orElse(ipAddress);

          ipAddress = privateIpv4AddressForENI;
        }

        containerInfos.add(ContainerInfo.builder()
                               .hostName(dockerId)
                               .ip(ipAddress)
                               .containerId(dockerId)
                               .ecsContainerDetails(ecsContainerDetailsBuilder.build())
                               .ec2Instance(ec2Instance)
                               .status(Status.SUCCESS)
                               .newContainer(!originalTaskArns.contains(taskForContainerInstance.getTaskArn()))
                               .containerTasksReachable(mainContainer != null)
                               .build());
      }
    }

    return containerInfos;
  }

  @NotNull
  EcsContainerDetailsBuilder initEcsContainerDetailsBuilder(Task taskForContainerInstance, Container mainContainer) {
    EcsContainerDetailsBuilder ecsContainerDetailsBuilder = EcsContainerDetails.builder();
    ecsContainerDetailsBuilder.containerInstanceArn(taskForContainerInstance.getContainerInstanceArn());
    ecsContainerDetailsBuilder.containerInstanceId(getIdFromArn(taskForContainerInstance.getContainerInstanceArn()));
    ecsContainerDetailsBuilder.taskArn(taskForContainerInstance.getTaskArn());
    ecsContainerDetailsBuilder.taskId(getIdFromArn(taskForContainerInstance.getTaskArn()));
    if (mainContainer != null) {
      ecsContainerDetailsBuilder.containerId(getIdFromArn(mainContainer.getContainerArn()));
      ecsContainerDetailsBuilder.dockerId(StringUtils.substring(mainContainer.getRuntimeId(), 0, 12));
      ecsContainerDetailsBuilder.completeDockerId(mainContainer.getRuntimeId());
    }
    return ecsContainerDetailsBuilder;
  }

  private List<ContainerInstance> fetchContainerInstancesForTasks(List<Task> tasks, String clusterName, String region,
      List<EncryptedDataDetail> encryptedDataDetails, AwsConfig awsConfig) {
    List<String> containerInstances =
        tasks.stream().map(Task::getContainerInstanceArn).filter(Objects::nonNull).collect(toList());
    logger.info("Container Instances = " + containerInstances);
    return awsHelperService
        .describeContainerInstances(region, awsConfig, encryptedDataDetails,
            new DescribeContainerInstancesRequest().withCluster(clusterName).withContainerInstances(containerInstances))
        .getContainerInstances();
  }

  EcsContainerDetailsBuilder getEcsContainerDetailsBuilder(Task ecsTask, Container container) {
    return EcsContainerDetails.builder()
        .taskArn(ecsTask.getTaskArn())
        .taskId(getIdFromArn(ecsTask.getTaskArn()))
        .dockerId(container != null ? StringUtils.substring(container.getRuntimeId(), 0, 12) : StringUtils.EMPTY)
        .completeDockerId(container != null ? container.getRuntimeId() : StringUtils.EMPTY);
  }

  /**
   * Ecs continually reports a message saying Service XYZ "has reached steady state."
   * The algorithm is pretty straigh forward.
   * Look at the deployments. If there are more that one deployments, no steady state.
   * else, the deployment.getUpdatedAt() >= last message with "has reached steady state."
   */
  @VisibleForTesting
  boolean hasServiceReachedSteadyState(Service service) {
    List<Deployment> deployments = service.getDeployments();
    if (deployments.size() != 1) {
      return false;
    }
    long deploymentTime = deployments.get(0).getUpdatedAt().getTime();
    long steadyStateMessageTime =
        service.getEvents()
            .stream()
            .filter(serviceEvent -> serviceEvent.getMessage().endsWith("has reached a steady state."))
            .map(serviceEvent -> serviceEvent.getCreatedAt().getTime())
            .max(Long::compare)
            .orElse(0L);
    return steadyStateMessageTime >= deploymentTime;
  }

  @Override
  public void waitForServiceToReachSteadyState(
      int serviceSteadyStateTimeout, UpdateServiceCountRequestData requestData) {
    ExecutionLogCallback executionLogCallback = requestData.getExecutionLogCallback();
    try {
      executionLogCallback.saveExecutionLog("Waiting for service to be in steady state...", LogLevel.INFO);
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          List<Service> services = getEcsServicesForCluster(requestData.getRegion(), requestData.getAwsConfig(),
              requestData.getEncryptedDataDetails(), requestData.getCluster(),
              Arrays.asList(requestData.getServiceName()));

          printAwsEvent(services.get(0), requestData.getServiceEvents(), requestData.getExecutionLogCallback());

          if (isNotEmpty(services)) {
            if (hasServiceReachedSteadyState(services.get(0))) {
              executionLogCallback.saveExecutionLog("Service has reached a steady state", LogLevel.INFO);
              return true;
            }
          }
          sleep(ofSeconds(10));
        }
      }, serviceSteadyStateTimeout, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String msg = new StringBuilder(128)
                       .append("Timed out waiting for service: ")
                       .append(requestData.getServiceName())
                       .append(" to reach steady state")
                       .toString();
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
      throw new TimeoutException(msg, "Timeout", e, WingsException.SRE);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        String msg = "Interrupted while waiting for service to reach steady state";
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
        throw new InvalidRequestException(msg, e);
      }
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private void waitForServiceUpdateToComplete(
      UpdateServiceResult updateServiceResult, UpdateServiceCountRequestData data) {
    final Service[] service = {updateServiceResult.getService()};
    ExecutionLogCallback executionLogCallback = data.getExecutionLogCallback();
    executionLogCallback.saveExecutionLog("Waiting for service: " + data.getServiceName()
            + " to reflect updated desired count: " + data.getDesiredCount(),
        LogLevel.INFO);
    try {
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          service[0] = getEcsServicesForCluster(data.getRegion(), data.getAwsConfig(), data.getEncryptedDataDetails(),
              data.getCluster(), Arrays.asList(data.getServiceName()))
                           .get(0);
          executionLogCallback.saveExecutionLog("Current service desired count return from aws for Service: "
                  + data.getServiceName() + " is: " + data.getDesiredCount(),
              LogLevel.INFO);
          if (Objects.equals(service[0].getDesiredCount(), Integer.valueOf(data.getDesiredCount()))) {
            return true;
          }
          sleep(ofSeconds(1));
        }
      }, 60L, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.warn("Service update failed {}", service[0]);
      executionLogCallback.saveExecutionLog(
          format("Timed out waiting for service desired count to match. expected: [%s], found [%s]",
              data.getDesiredCount(), service[0].getDesiredCount()),
          LogLevel.ERROR);
      executionLogCallback.saveExecutionLog("Service resize operation failed.", LogLevel.ERROR);
      throw new InvalidRequestException("Service update timed out");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Service update failed", e);
    }
  }

  private void printAwsEvent(
      Service service, List<ServiceEvent> eventsAlreadyProcessed, ExecutionLogCallback executionLogCallback) {
    List<ServiceEvent> events = new ArrayList<>();
    events.addAll(service.getEvents());

    Set<String> eventIdsProcessed = eventsAlreadyProcessed.stream().map(ServiceEvent::getId).collect(toSet());
    events = events.stream().filter(event -> !eventIdsProcessed.contains(event.getId())).collect(toList());

    events.forEach(event
        -> executionLogCallback.saveExecutionLog(
            color(new StringBuilder(128).append("# AWS Event: ").append(event.getMessage()).toString(), Yellow, Bold)));

    eventsAlreadyProcessed.addAll(events);
  }

  @Override
  public void createService(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, CreateServiceRequest clusterConfiguration) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);
    awsHelperService.createService(region, awsConfig, encryptedDataDetails, clusterConfiguration);
  }

  @Override
  public TaskDefinition createTask(String region, SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(settingAttribute, encryptedDataDetails);
    return awsHelperService
        .registerTaskDefinition(region, awsConfig, encryptedDataDetails, registerTaskDefinitionRequest)
        .getTaskDefinition();
  }

  @Override
  public List<Service> getServices(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);
    List<Service> services = new ArrayList<>();
    ListServicesResult listServicesResult;
    ListServicesRequest listServicesRequest = new ListServicesRequest().withCluster(clusterName);
    do {
      listServicesResult = awsHelperService.listServices(region, awsConfig, encryptedDataDetails, listServicesRequest);
      if (isEmpty(listServicesResult.getServiceArns())) {
        break;
      }

      services.addAll(getEcsServicesForCluster(
          region, awsConfig, encryptedDataDetails, clusterName, listServicesResult.getServiceArns()));
      listServicesRequest.setNextToken(listServicesResult.getNextToken());
    } while (listServicesResult.getNextToken() != null && listServicesResult.getServiceArns().size() == 10);

    return services;
  }

  @Override
  public List<ServiceEvent> getServiceEvents(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName) {
    List<Service> services =
        getEcsServicesForCluster(region, awsConfig, encryptedDataDetails, clusterName, Arrays.asList(serviceName));

    if (isNotEmpty(services) && isNotEmpty(services.get(0).getEvents())) {
      return new ArrayList<>(services.get(0).getEvents());
    }

    return new ArrayList<>();
  }

  @Override
  public List<ServiceEvent> getEventsFromService(Service service) {
    List<ServiceEvent> serviceEvents = new ArrayList<>();
    if (service != null && isNotEmpty(service.getEvents())) {
      serviceEvents.addAll(service.getEvents());
    }

    return serviceEvents;
  }

  @Override
  public TargetGroup getTargetGroup(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String targetGroupArn) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);
    return awsHelperService.getTargetGroupForAlb(region, awsConfig, encryptedDataDetails, targetGroupArn);
  }

  @Override
  public TaskDefinition getTaskDefinitionFromService(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, Service service) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(cloudProviderSetting, encryptedDataDetails);
    DescribeTaskDefinitionRequest describeTaskDefinitionRequest =
        new DescribeTaskDefinitionRequest().withTaskDefinition(service.getTaskDefinition());
    DescribeTaskDefinitionResult taskDefinitionResult =
        awsHelperService.describeTaskDefinition(region, awsConfig, encryptedDataDetails, describeTaskDefinitionRequest);
    return taskDefinitionResult.getTaskDefinition();
  }

  private List<String> getTaskArns(String region, List<EncryptedDataDetail> encryptedDataDetails, String clusterName,
      String serviceName, AwsConfig awsConfig, DesiredStatus desiredStatus) {
    return awsHelperService
        .listTasks(region, awsConfig, encryptedDataDetails,
            new ListTasksRequest()
                .withCluster(clusterName)
                .withServiceName(serviceName)
                .withDesiredStatus(desiredStatus))
        .getTaskArns();
  }

  private List<Service> getEcsServicesForCluster(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, List<String> serviceName) {
    return awsHelperService
        .describeServices(region, awsConfig, encryptedDataDetails,
            new DescribeServicesRequest().withCluster(clusterName).withServices(serviceName))
        .getServices();
  }
}
