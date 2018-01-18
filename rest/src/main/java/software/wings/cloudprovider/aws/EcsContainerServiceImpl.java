package software.wings.cloudprovider.aws;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.awaitility.Awaitility.with;
import static org.hamcrest.core.IsEqual.equalTo;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;

import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceEvent;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.fluent.Request;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.utils.HttpUtil;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by anubhaw on 12/28/16.
 */
@Singleton
public class EcsContainerServiceImpl implements EcsContainerService {
  private static final java.time.Duration SLEEP_INTERVAL = ofSeconds(10);
  private static final long RETRY_COUNTER = ofMinutes(10).getSeconds() / SLEEP_INTERVAL.getSeconds();
  private static final Logger logger = LoggerFactory.getLogger(EcsContainerServiceImpl.class);
  @Inject private AwsHelperService awsHelperService = new AwsHelperService();
  private ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  /**
   * Create cluster.
   */
  public void createCluster() {
    CreateStackResult result = awsHelperService.createStack("AKIAJLEKM45P4PO5QUFQ",
        "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE".toCharArray(),
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
                new Parameter().withParameterKey("VpcId").withParameterValue("vpc-84a9bfe0")));
    result.getStackId();

    Stack stack;
    while (!"CREATE_COMPLETE".equals(
        (stack = awsHelperService
                     .describeStacks("AKIAJLEKM45P4PO5QUFQ", "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE".toCharArray(),
                         new DescribeStacksRequest().withStackName("EC2ContainerService-test2"))
                     .getStacks()
                     .get(0))
            .getStackStatus())) {
      sleep(ofSeconds(1));
    }

    stack.getOutputs().forEach(output -> System.out.println(output.getOutputKey() + " = " + output.getOutputValue()));
  }

  /**
   * Destroy cluster.
   */
  public void destroyCluster() {
    CreateStackResult result = awsHelperService.createStack("AKIAJLEKM45P4PO5QUFQ",
        "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE".toCharArray(),
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
                new Parameter().withParameterKey("VpcId").withParameterValue("vpc-84a9bfe0")));

    result.getStackId();

    Stack stack;
    while (!"CREATE_COMPLETE".equals(
        (stack = awsHelperService
                     .describeStacks("AKIAJLEKM45P4PO5QUFQ", "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE".toCharArray(),
                         new DescribeStacksRequest().withStackName("EC2ContainerService-test2"))
                     .getStacks()
                     .get(0))
            .getStackStatus())) {
      sleep(ofSeconds(1));
    }

    stack.getOutputs().forEach(output -> System.out.println(output.getOutputKey() + " = " + output.getOutputValue()));
  }

  @Override
  public void provisionNodes(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, Integer clusterSize, String launchConfigName,
      Map<String, Object> params) {
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
            .withVPCZoneIdentifier(vpcZoneIdentifiers));

    logger.info("Successfully created autoScalingGroup: {}", autoScalingGroupName);

    waitForAllInstancesToBeReady(awsConfig, encryptedDataDetails, region, autoScalingGroupName, clusterSize);
    waitForAllInstanceToRegisterWithCluster(region, awsConfig, encryptedDataDetails, clusterName, clusterSize);

    logger.info("All instances are ready for deployment");
  }

  private void waitForAllInstanceToRegisterWithCluster(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, Integer clusterSize) {
    long retryCount = RETRY_COUNTER;
    while (!allInstancesRegisteredWithCluster(region, awsConfig, encryptedDataDetails, clusterName, clusterSize)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT).addParam("message", "All instances didn't registered with cluster");
      }
      sleep(SLEEP_INTERVAL);
    }
  }

  private void waitForAllInstancesToBeReady(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String autoscalingGroupName, Integer clusterSize) {
    long retryCount = RETRY_COUNTER;
    while (!allInstanceInReadyState(awsConfig, encryptedDataDetails, region, autoscalingGroupName, clusterSize)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT)
            .addParam("message", "Not all instances ready to registered with cluster");
      }
      sleep(SLEEP_INTERVAL);
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
      throw new WingsException(INVALID_REQUEST, ex).addParam("message", ex.getMessage());
    }
    logger.info("Begin service deployment " + createServiceRequest.getServiceName());
    CreateServiceResult createServiceResult =
        awsHelperService.createService(region, awsConfig, encryptedDataDetails, createServiceRequest);

    waitForTasksToBeInRunningState(region, awsConfig, encryptedDataDetails, createServiceRequest.getCluster(),
        createServiceRequest.getServiceName(), new ExecutionLogCallback());

    return createServiceResult.getService().getServiceArn();
  }

  private void waitForTasksToBeInRunningState(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      ExecutionLogCallback executionLogCallback) {
    long retryCount = RETRY_COUNTER;
    while (!allDesiredTaskRunning(
        region, awsConfig, encryptedDataDetails, clusterName, serviceName, executionLogCallback)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT).addParam("message", "Some tasks are still not in running state");
      }
      sleep(SLEEP_INTERVAL);
    }
  }

  private void waitForTasksToBeInRunningStateButDontThrowException(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      ExecutionLogCallback executionLogCallback) {
    long retryCount = RETRY_COUNTER;
    while (!allDesiredTaskRunning(
        region, awsConfig, encryptedDataDetails, clusterName, serviceName, executionLogCallback)) {
      if (retryCount-- <= 0) {
        break;
      }
      sleep(SLEEP_INTERVAL);
    }
  }

  private boolean allDesiredTaskRunning(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      ExecutionLogCallback executionLogCallback) {
    Service service = awsHelperService
                          .describeServices(region, awsConfig, encryptedDataDetails,
                              new DescribeServicesRequest().withCluster(clusterName).withServices(serviceName))
                          .getServices()
                          .get(0);

    logger.info(
        "Waiting for pending tasks to finish. {}/{} running ...", service.getRunningCount(), service.getDesiredCount());

    executionLogCallback.saveExecutionLog(String.format("Waiting for pending tasks to finish. %s/%s running ...",
                                              service.getRunningCount(), service.getDesiredCount()),
        LogLevel.INFO);
    return service.getDesiredCount().equals(service.getRunningCount());
  }

  @Override
  public void deleteService(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);
    awsHelperService.deleteService(region, awsConfig, encryptedDataDetails,
        new DeleteServiceRequest().withCluster(clusterName).withService(serviceName));
  }

  @Override
  public List<ContainerInfo> provisionTasks(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName, int previousCount,
      int desiredCount, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);

    try {
      if (desiredCount != previousCount) {
        UpdateServiceRequest updateServiceRequest =
            new UpdateServiceRequest().withCluster(clusterName).withService(serviceName).withDesiredCount(desiredCount);
        UpdateServiceResult updateServiceResult =
            awsHelperService.updateService(region, awsConfig, encryptedDataDetails, updateServiceRequest);
        List<ServiceEvent> events = updateServiceResult.getService().getEvents();

        String latestExcludedEventId = null;
        if (!events.isEmpty()) {
          latestExcludedEventId = events.get(0).getId();
        }

        waitForServiceUpdateToComplete(updateServiceResult, region, awsConfig, encryptedDataDetails, clusterName,
            serviceName, desiredCount, executionLogCallback);
        executionLogCallback.saveExecutionLog("Service update request successfully submitted.", LogLevel.INFO);
        waitForTasksToBeInRunningStateButDontThrowException(
            region, awsConfig, encryptedDataDetails, clusterName, serviceName, executionLogCallback);
        if (desiredCount > previousCount) { // don't do it for downsize.
          waitForServiceToReachSteadyState(latestExcludedEventId, region, awsConfig, encryptedDataDetails, clusterName,
              serviceName, serviceSteadyStateTimeout, executionLogCallback);
        }
      }

      List<String> taskArns = awsHelperService
                                  .listTasks(region, awsConfig, encryptedDataDetails,
                                      new ListTasksRequest().withCluster(clusterName).withServiceName(serviceName))
                                  .getTaskArns();
      if (isEmpty(taskArns)) {
        logger.info("No task arns.");
        return emptyList();
      }

      logger.info("Task arns = " + taskArns);
      List<Task> tasks = awsHelperService
                             .describeTasks(region, awsConfig, encryptedDataDetails,
                                 new DescribeTasksRequest().withCluster(clusterName).withTasks(taskArns))
                             .getTasks();
      List<String> containerInstances = tasks.stream().map(Task::getContainerInstanceArn).collect(Collectors.toList());
      logger.info("Container Instances = " + containerInstances);

      List<ContainerInstance> containerInstanceList =
          awsHelperService
              .describeContainerInstances(region, awsConfig, encryptedDataDetails,
                  new DescribeContainerInstancesRequest()
                      .withCluster(clusterName)
                      .withContainerInstances(containerInstances))
              .getContainerInstances();
      List<ContainerInfo> containerInfos = new ArrayList<>();
      containerInstanceList.forEach(containerInstance -> {
        com.amazonaws.services.ec2.model.Instance ec2Instance =
            awsHelperService
                .describeEc2Instances(awsConfig, encryptedDataDetails, region,
                    new DescribeInstancesRequest().withInstanceIds(containerInstance.getEc2InstanceId()))
                .getReservations()
                .get(0)
                .getInstances()
                .get(0);
        String ipAddress = ec2Instance.getPrivateIpAddress();
        String uri = "http://" + ipAddress + ":51678/v1/tasks";
        if (HttpUtil.connectableHttpUrl(uri)) {
          try {
            executionLogCallback.saveExecutionLog("Fetching container meta data from " + uri, LogLevel.INFO);
            logger.info("requesting data from {}", uri);
            TaskMetadata taskMetadata = Request.Get(uri).execute().handleResponse(response
                -> JsonUtils.asObject(CharStreams.toString(new InputStreamReader(response.getEntity().getContent())),
                    TaskMetadata.class));

            taskMetadata.getTasks()
                .stream()
                .filter(task -> taskArns.contains(task.getArn()))
                .findFirst()
                .ifPresent(task -> {
                  String containerId = StringUtils.substring(task.getContainers().get(0).getDockerId(), 0, 12);
                  ContainerInfo containerInfo = ContainerInfo.builder()
                                                    .hostName(containerId)
                                                    .containerId(containerId)
                                                    .ec2Instance(ec2Instance)
                                                    .status(Status.SUCCESS)
                                                    .build();
                  containerInfos.add(containerInfo);
                  executionLogCallback.saveExecutionLog(
                      "Container docker ID: " + containerInfo.getContainerId(), LogLevel.INFO);

                });
            logger.info("TaskMetadata = " + taskMetadata);
          } catch (IOException ex) {
            executionLogCallback.saveExecutionLog(
                "Could not fetch container meta data. Verification steps using containerId may not work",
                LogLevel.WARN);
            logger.error("Container meta data fetch failed on EC2 host: " + ipAddress, ex);
            containerInfos.add(ContainerInfo.builder()
                                   .hostName(ipAddress)
                                   .containerId(ipAddress)
                                   .ec2Instance(ec2Instance)
                                   .status(Status.SUCCESS)
                                   .build());
          } catch (Exception e) {
            logger.error("Unknown error fetching meta info ", e);
            throw new WingsException(INVALID_REQUEST, e).addParam("message", e.getMessage());
          }
        } else {
          logger.warn("Could not connect to {}", uri);
          executionLogCallback.saveExecutionLog("Could not reach " + uri
                  + " to fetch container meta data. Verification steps using containerId may not work",
              LogLevel.WARN);
          containerInfos.add(ContainerInfo.builder()
                                 .hostName(ipAddress)
                                 .containerId(ipAddress)
                                 .ec2Instance(ec2Instance)
                                 .status(Status.SUCCESS)
                                 .build());
        }
      });
      logger.info("Docker container ids = " + containerInfos);
      return containerInfos;
    } catch (Exception ex) {
      throw new WingsException(INVALID_REQUEST, ex).addParam("message", ex.getMessage());
    }
  }

  private void waitForServiceToReachSteadyState(String latestExcludedEventId, String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    long retryCount = (serviceSteadyStateTimeout * 60) / SLEEP_INTERVAL.getSeconds();

    if (retryCount == 0) {
      return;
    }

    try {
      final String[] excludedEventId = {latestExcludedEventId};
      do {
        executionLogCallback.saveExecutionLog("Waiting for service to be in steady state...", LogLevel.INFO);
        Service service = awsHelperService
                              .describeServices(region, awsConfig, encryptedDataDetails,
                                  new DescribeServicesRequest().withCluster(clusterName).withServices(serviceName))
                              .getServices()
                              .get(0);
        List<ServiceEvent> events = service.getEvents();
        int excludedEndIndex = IntStream.range(0, events.size())
                                   .filter(idx -> events.get(idx).getId().equals(excludedEventId[0]))
                                   .findFirst()
                                   .orElse(events.size());

        for (int i = excludedEndIndex - 1; i >= 0; i--) {
          executionLogCallback.saveExecutionLog("EVENT: " + events.get(i).getMessage(), LogLevel.INFO);
          if (events.get(i).getMessage().endsWith("has reached a steady state.")) {
            executionLogCallback.saveExecutionLog("Service has reached a steady state", LogLevel.INFO);
            return;
          }
        }
        if (!events.isEmpty()) {
          excludedEventId[0] = events.get(0).getId();
        }

        sleep(SLEEP_INTERVAL);
      } while (retryCount-- > 0);
    } catch (Exception ex) {
      logger.error("Wait for service steady state failed with exception ", ex);
      if (ex instanceof InterruptedException) {
        String msg = "Timed out waiting for service to reach steady state.";
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
        throw new WingsException(INVALID_REQUEST).addParam("message", msg);
      }
      throw new WingsException(INVALID_REQUEST, ex).addParam("message", ex.getMessage());
    }
    executionLogCallback.saveExecutionLog("Service failed to reach a steady state", LogLevel.ERROR);
    throw new WingsException(INVALID_REQUEST).addParam("message", "Service failed to reach a steady state");
  }

  private void waitForServiceUpdateToComplete(UpdateServiceResult updateServiceResult, String region,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      Integer desiredCount, ExecutionLogCallback executionLogCallback) {
    final Service[] service = {updateServiceResult.getService()};
    try {
      with().pollInterval(10L, TimeUnit.SECONDS).atMost(new Duration(60L, TimeUnit.SECONDS)).until(() -> {
        service[0] = awsHelperService
                         .describeServices(region, awsConfig, encryptedDataDetails,
                             new DescribeServicesRequest().withCluster(clusterName).withServices(serviceName))
                         .getServices()
                         .get(0);
        return Objects.equals(service[0].getDesiredCount(), desiredCount);
      }, equalTo(true));
    } catch (ConditionTimeoutException e) {
      logger.warn("Service update failed {}", service[0]);
      executionLogCallback.saveExecutionLog(
          String.format("Service desired count didn't match. expected: [%s], found [%s]", desiredCount,
              service[0].getDesiredCount()),
          LogLevel.ERROR);
      executionLogCallback.saveExecutionLog("Service resize operation failed.", LogLevel.ERROR);
      throw new WingsException(INVALID_REQUEST).addParam("message", "Service update failed");
    }
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
      services.addAll(awsHelperService
                          .describeServices(region, awsConfig, encryptedDataDetails,
                              new DescribeServicesRequest()
                                  .withCluster(clusterName)
                                  .withServices(listServicesResult.getServiceArns()))
                          .getServices());
      listServicesRequest.setNextToken(listServicesResult.getNextToken());
    } while (listServicesResult.getNextToken() != null && listServicesResult.getServiceArns().size() == 10);

    return services;
  }
}
