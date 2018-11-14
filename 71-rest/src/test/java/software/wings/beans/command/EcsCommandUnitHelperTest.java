package software.wings.beans.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TASK_FAMILY;
import static software.wings.utils.WingsTestConstants.TASK_REVISION;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceRegistry;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EcsCommandUnitHelperTest extends WingsBaseTest {
  public static final String SECURITY_GROUP_ID_1 = "sg-id";
  public static final String CLUSTER_NAME = "clusterName";
  public static final String TARGET_GROUP_ARN = "targetGroupArn";
  public static final String SUBNET_ID = "subnet-id";
  public static final String VPC_ID = "vpc-id";
  public static final String CONTAINER_SERVICE_NAME = "containerServiceName";
  public static final String CONTAINER_NAME = "containerName";
  public static final String ROLE_ARN = "taskToleArn";
  public static final String DOCKER_IMG_NAME = "dockerImgName";
  public static final String DOCKER_DOMAIN_NAME = "dockerDomainName";
  @Mock private AwsClusterService awsClusterService;
  @Mock private AwsAppAutoScalingHelperServiceDelegate awsAppAutoScalingServiceDelegate;
  @InjectMocks @Inject private EcsSetupCommandUnit ecsSetupCommandUnit;
  @Inject private EcsCommandUnitHelper ecsCommandUnitHelper;
  private static Logger logger = LoggerFactory.getLogger(EcsSetupCommandUnitTest.class);

  private EcsSetupParams setupParams =
      anEcsSetupParams()
          .withAppName(APP_NAME)
          .withEnvName(ENV_NAME)
          .withServiceName(SERVICE_NAME)
          .withImageDetails(
              ImageDetails.builder().registryUrl("ecr").sourceName("ECR").name("todolist").tag("v1").build())
          .withInfraMappingId(INFRA_MAPPING_ID)
          .withRegion(Regions.US_EAST_1.getName())
          .withRoleArn("roleArn")
          .withTargetGroupArn("targetGroupArn")
          .withTaskFamily(TASK_FAMILY)
          .withUseLoadBalancer(false)
          .withClusterName("cluster")
          .build();
  private SettingAttribute computeProvider = aSettingAttribute().withValue(GcpConfig.builder().build()).build();

  private String ecsSErviceSpecJsonString = "{\n"
      + "\"placementConstraints\":[],\n"
      + "\"placementStrategy\":[],\n"
      + "\"healthCheckGracePeriodSeconds\":null,\n"
      + "\"serviceRegistries\": [\n"
      + "                {\n"
      + "                    \"registryArn\": \"arn:aws:servicediscovery:us-east-1:448640225317:service/srv-v43my342legaqd3r\",\n"
      + "                    \"containerName\": \"containerName\",\n"
      + "                    \"containerPort\": 80\n"
      + "                }\n"
      + "            ],\n"
      + "\"schedulingStrategy\":\"REPLICA\"\n"
      + "}";

  private static String scalingPolicyJson = "{\n"
      + "            \"policyName\": \"TrackingPolicyTest\",\n"
      + "            \"policyType\": \"TargetTrackingScaling\",\n"
      + "            \"targetTrackingScalingPolicyConfiguration\": {\n"
      + "                \"targetValue\": 60.0,\n"
      + "                \"predefinedMetricSpecification\": {\n"
      + "                    \"predefinedMetricType\": \"ECSServiceAverageCPUUtilization\"\n"
      + "                },\n"
      + "                \"scaleOutCooldown\": 300,\n"
      + "                \"scaleInCooldown\": 300\n"
      + "            }"
      + "        }";

  private static String registerTargetJson = "{\n"
      + "            \"scalableDimension\": \"ecs:service:DesiredCount\",\n"
      + "            \"minCapacity\": 1,\n"
      + "            \"maxCapacity\": 3,\n"
      + "            \"roleARN\": \"arn:aws:iam::448640225317:role/aws-service-role/ecs.application-autoscaling.amazonaws.com/AWSServiceRoleForApplicationAutoScaling_ECSService\"\n"
      + "        }";

  private TaskDefinition taskDefinition;

  /**
   * Set up.
   */
  @Before
  public void setup() {
    taskDefinition = new TaskDefinition();
    taskDefinition.setFamily(TASK_FAMILY);
    taskDefinition.setRevision(TASK_REVISION);

    when(awsClusterService.createTask(eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(),
             any(RegisterTaskDefinitionRequest.class)))
        .thenReturn(taskDefinition);
  }

  @Test
  public void testGetCreateServiceRequest_Fargate() throws Exception {
    EcsSetupParams setupParams = anEcsSetupParams()
                                     .withClusterName(CLUSTER_NAME)
                                     .withTargetGroupArn(TARGET_GROUP_ARN)
                                     .withRoleArn(ROLE_ARN)
                                     .withRegion(Regions.US_EAST_1.getName())
                                     .withAssignPublicIps(true)
                                     .withVpcId(VPC_ID)
                                     .withSecurityGroupIds(new String[] {SECURITY_GROUP_ID_1})
                                     .withSubnetIds(new String[] {SUBNET_ID})
                                     .withExecutionRoleArn("arn")
                                     .withUseLoadBalancer(true)
                                     .withLaunchType(LaunchType.FARGATE.name())
                                     .build();

    TaskDefinition taskDefinition =
        new TaskDefinition()
            .withRequiresCompatibilities(LaunchType.FARGATE.name())
            .withExecutionRoleArn("arn")
            .withFamily("family")
            .withRevision(1)
            .withContainerDefinitions(
                new com.amazonaws.services.ecs.model.ContainerDefinition()
                    .withPortMappings(new PortMapping().withContainerPort(80).withProtocol("http"))
                    .withName(CONTAINER_NAME));

    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    TargetGroup targetGroup = new TargetGroup();
    targetGroup.setPort(80);
    targetGroup.setTargetGroupArn(TARGET_GROUP_ARN);

    when(awsClusterService.getTargetGroup(
             Regions.US_EAST_1.getName(), computeProvider, Collections.emptyList(), TARGET_GROUP_ARN))
        .thenReturn(targetGroup);

    CreateServiceRequest createServiceRequest =
        ecsCommandUnitHelper.getCreateServiceRequest(computeProvider, encryptedDataDetails, setupParams, taskDefinition,
            CONTAINER_SERVICE_NAME, awsClusterService, executionLogCallback, logger);

    assertNotNull(createServiceRequest);

    // Required for fargate using Load balancer, as ECS assumes role automatically
    assertNull(createServiceRequest.getRole());

    assertNotNull(createServiceRequest.getNetworkConfiguration());
    assertNotNull(createServiceRequest.getNetworkConfiguration().getAwsvpcConfiguration());
    assertEquals(LaunchType.FARGATE.name(), createServiceRequest.getLaunchType());

    AwsVpcConfiguration awsvpcConfiguration = createServiceRequest.getNetworkConfiguration().getAwsvpcConfiguration();
    assertEquals(AssignPublicIp.ENABLED.name(), awsvpcConfiguration.getAssignPublicIp());
    assertEquals(1, awsvpcConfiguration.getSecurityGroups().size());
    assertEquals(SECURITY_GROUP_ID_1, awsvpcConfiguration.getSecurityGroups().iterator().next());
    assertEquals(1, awsvpcConfiguration.getSubnets().size());
    assertEquals(SUBNET_ID, awsvpcConfiguration.getSubnets().iterator().next());

    assertEquals(CONTAINER_SERVICE_NAME, createServiceRequest.getServiceName());
    assertEquals(CLUSTER_NAME, createServiceRequest.getCluster());
    assertEquals(0, createServiceRequest.getDesiredCount().intValue());

    assertNotNull(createServiceRequest.getDeploymentConfiguration());
    assertEquals(100, createServiceRequest.getDeploymentConfiguration().getMinimumHealthyPercent().intValue());
    assertEquals(200, createServiceRequest.getDeploymentConfiguration().getMaximumPercent().intValue());

    assertEquals(
        taskDefinition.getFamily() + ":" + taskDefinition.getRevision(), createServiceRequest.getTaskDefinition());

    assertNotNull(createServiceRequest.getLoadBalancers());
    assertEquals(1, createServiceRequest.getLoadBalancers().size());
    LoadBalancer loadBalancer = createServiceRequest.getLoadBalancers().iterator().next();
    assertEquals(CONTAINER_NAME, loadBalancer.getContainerName());
    assertEquals(TARGET_GROUP_ARN, loadBalancer.getTargetGroupArn());
    assertEquals(80, loadBalancer.getContainerPort().intValue());
  }

  @Test
  public void testGetCreateServiceRequest_EC2() throws Exception {
    EcsSetupParams setupParams = getEcsSetupParams();
    TaskDefinition taskDefinition = getTaskDefinition();

    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    TargetGroup targetGroup = getTargetGroup();

    when(awsClusterService.getTargetGroup(
             Regions.US_EAST_1.getName(), computeProvider, Collections.emptyList(), TARGET_GROUP_ARN))
        .thenReturn(targetGroup);

    CreateServiceRequest createServiceRequest =
        ecsCommandUnitHelper.getCreateServiceRequest(computeProvider, encryptedDataDetails, setupParams, taskDefinition,
            CONTAINER_SERVICE_NAME, awsClusterService, executionLogCallback, logger);

    assertCreateServiceRequestObject(taskDefinition, createServiceRequest);
  }

  @Test
  public void testGetCreateServiceRequest_serviceSpec() throws Exception {
    EcsSetupParams setupParams = getEcsSetupParams();
    setupParams.setUseLoadBalancer(false);
    setupParams.setTargetGroupArn(null);
    setupParams.setLoadBalancerName(null);

    setupParams.setEcsServiceSpecification(
        EcsServiceSpecification.builder().serviceId(SERVICE_ID).serviceSpecJson(ecsSErviceSpecJsonString).build());
    TaskDefinition taskDefinition = getTaskDefinition();

    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    CreateServiceRequest createServiceRequest =
        ecsCommandUnitHelper.getCreateServiceRequest(computeProvider, encryptedDataDetails, setupParams, taskDefinition,
            CONTAINER_SERVICE_NAME, awsClusterService, executionLogCallback, logger);

    List<ServiceRegistry> serviceRegistries = createServiceRequest.getServiceRegistries();
    assertNotNull(createServiceRequest.getServiceRegistries());
    assertEquals(1, serviceRegistries.size());
    ServiceRegistry serviceRegistry = createServiceRequest.getServiceRegistries().get(0);
    assertNotNull(serviceRegistry);
    assertEquals("arn:aws:servicediscovery:us-east-1:448640225317:service/srv-v43my342legaqd3r",
        serviceRegistry.getRegistryArn());
    assertEquals(CONTAINER_NAME, serviceRegistry.getContainerName());
    assertEquals(80, serviceRegistry.getContainerPort().intValue());
  }

  @Test
  public void testValidateServiceRegistries() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    Service service = ecsCommandUnitHelper.getAwsServiceFromJson(ecsSErviceSpecJsonString, logger);
    assertNotNull(service);
    assertNotNull(service.getServiceRegistries());
    assertEquals(1, service.getServiceRegistries().size());

    // Valid case
    ecsCommandUnitHelper.validateServiceRegistries(
        service.getServiceRegistries(), getTaskDefinition(), executionLogCallback);

    // Invalid cases
    service.getServiceRegistries().get(0).setContainerName("invalid");
    TaskDefinition taskDefinition = getTaskDefinition();
    try {
      ecsCommandUnitHelper.validateServiceRegistries(
          service.getServiceRegistries(), taskDefinition, executionLogCallback);
      assertFalse(false);
    } catch (Exception e) {
      assertTrue(true);
    }

    service.getServiceRegistries().get(0).setContainerName(CONTAINER_NAME);
    service.getServiceRegistries().get(0).setContainerPort(2000);
    try {
      ecsCommandUnitHelper.validateServiceRegistries(
          service.getServiceRegistries(), getTaskDefinition(), executionLogCallback);
      assertFalse(false);
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  private void assertCreateServiceRequestObject(
      TaskDefinition taskDefinition, CreateServiceRequest createServiceRequest) {
    assertNotNull(createServiceRequest);

    // netWorkConfiguration should be ignored here, as its required only for fargate
    assertNotNull(createServiceRequest.getRole());
    assertEquals(ROLE_ARN, createServiceRequest.getRole());
    assertNull(createServiceRequest.getNetworkConfiguration());
    assertEquals(CONTAINER_SERVICE_NAME, createServiceRequest.getServiceName());
    assertEquals(CLUSTER_NAME, createServiceRequest.getCluster());
    assertEquals(0, createServiceRequest.getDesiredCount().intValue());
    assertNotNull(createServiceRequest.getDeploymentConfiguration());
    assertEquals(100, createServiceRequest.getDeploymentConfiguration().getMinimumHealthyPercent().intValue());
    assertEquals(200, createServiceRequest.getDeploymentConfiguration().getMaximumPercent().intValue());
    assertEquals(
        taskDefinition.getFamily() + ":" + taskDefinition.getRevision(), createServiceRequest.getTaskDefinition());
    assertNotNull(createServiceRequest.getLoadBalancers());
    assertEquals(1, createServiceRequest.getLoadBalancers().size());

    LoadBalancer loadBalancer = createServiceRequest.getLoadBalancers().iterator().next();
    assertEquals(CONTAINER_NAME, loadBalancer.getContainerName());
    assertEquals(TARGET_GROUP_ARN, loadBalancer.getTargetGroupArn());
    assertEquals(80, loadBalancer.getContainerPort().intValue());
  }

  private TargetGroup getTargetGroup() {
    TargetGroup targetGroup = new TargetGroup();
    targetGroup.setPort(80);
    targetGroup.setTargetGroupArn(TARGET_GROUP_ARN);
    return targetGroup;
  }

  private EcsSetupParams getEcsSetupParams() {
    return anEcsSetupParams()
        .withClusterName(CLUSTER_NAME)
        .withTargetGroupArn(TARGET_GROUP_ARN)
        .withRoleArn(ROLE_ARN)
        .withRegion(Regions.US_EAST_1.getName())
        .withUseLoadBalancer(true)
        .withImageDetails(ImageDetails.builder().name("ImageName").tag("Tag").build())
        .build();
  }

  @Test
  public void testGetCreateServiceRequest_EC2_awsvpc() throws Exception {
    EcsSetupParams setupParams = getEcsSetupParams();
    setupParams.setSubnetIds(new String[] {"subnet1"});
    setupParams.setSecurityGroupIds(new String[] {"sg1"});
    setupParams.setAssignPublicIps(true);

    TaskDefinition taskDefinition = getTaskDefinition();
    taskDefinition.setNetworkMode("awsvpc");

    TargetGroup targetGroup = getTargetGroup();

    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    when(awsClusterService.getTargetGroup(
             Regions.US_EAST_1.getName(), computeProvider, Collections.emptyList(), TARGET_GROUP_ARN))
        .thenReturn(targetGroup);

    CreateServiceRequest createServiceRequest =
        ecsCommandUnitHelper.getCreateServiceRequest(computeProvider, encryptedDataDetails, setupParams, taskDefinition,
            CONTAINER_SERVICE_NAME, awsClusterService, executionLogCallback, logger);

    assertNotNull(createServiceRequest.getNetworkConfiguration());
    assertNotNull(createServiceRequest.getNetworkConfiguration().getAwsvpcConfiguration());
    AwsVpcConfiguration awsVpcConfiguration = createServiceRequest.getNetworkConfiguration().getAwsvpcConfiguration();
    assertEquals(1, awsVpcConfiguration.getSecurityGroups().size());
    assertEquals("sg1", awsVpcConfiguration.getSecurityGroups().get(0));
    assertEquals(1, awsVpcConfiguration.getSubnets().size());
    assertEquals("subnet1", awsVpcConfiguration.getSubnets().get(0));
    assertEquals(AssignPublicIp.DISABLED.name(), awsVpcConfiguration.getAssignPublicIp());
  }

  private TaskDefinition getTaskDefinition() {
    return new TaskDefinition().withFamily("family").withRevision(1).withContainerDefinitions(
        new com.amazonaws.services.ecs.model.ContainerDefinition()
            .withPortMappings(new PortMapping().withContainerPort(80).withProtocol("http"))
            .withName(CONTAINER_NAME));
  }

  //  @Test
  //  public void TestSerializeJsonToAwsClasses() {
  //    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
  //    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());
  //    PutScalingPolicyRequest putScalingPolicyRequest =
  //        ecsCommandUnitHelper.getPutScalingPolicyRequestFromJson(scalingPolicyJson, executionLogCallback);
  //    RegisterScalableTargetRequest registerScalableTargetRequest =
  //        ecsCommandUnitHelper.getRegisterScalableTargetRequestFromJson(registerTargetJson, executionLogCallback);
  //    registerScalableTargetRequest.setServiceNamespace(ServiceNamespace.Ecs);
  //    registerScalableTargetRequest.setResourceId("service/cluster/serviceName");
  //    int i = 0;
  //  }
}
