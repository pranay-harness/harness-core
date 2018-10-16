package software.wings.beans.command;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TASK_FAMILY;
import static software.wings.utils.WingsTestConstants.TASK_REVISION;

import com.google.common.collect.Lists;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.NetworkMode;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.EcsConvention;
import software.wings.utils.WingsTestConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class EcsSetupCommandUnitTest extends WingsBaseTest {
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

  @InjectMocks private EcsSetupCommandUnit ecsSetupCommandUnit = new EcsSetupCommandUnit();

  private final String fargateConfigYaml = "{\n"
      + "  \"networkMode\": \"awsvpc\", \n"
      + "  \"taskRoleArn\":null,\n"
      + "  \"executionRoleArn\": \"arn:aws:iam::830767422336:role/ecsTaskExecutionRole\", \n"
      + "  \"containerDefinitions\" : [ {\n"
      + "    \"logConfiguration\": {\n"
      + "        \"logDriver\": \"awslogs\",\n"
      + "        \"options\": {\n"
      + "          \"awslogs-group\": \"/ecs/test_3__fargate__env\",\n"
      + "          \"awslogs-region\": \"us-east-1\",\n"
      + "          \"awslogs-stream-prefix\": \"ecs\"\n"
      + "        }\n"
      + "    },\n"
      + "    \"name\" : \"${CONTAINER_NAME}\",\n"
      + "    \"image\" : \"${DOCKER_IMAGE_NAME}\",\n"
      + "    \"links\" : [ ],\n"
      + "    \"cpu\": 256, \n"
      + "    \"memoryReservation\": 1024, \n"
      + "    \"portMappings\": [\n"
      + "                {\n"
      + "                    \"containerPort\": 80,\n"
      + "                    \"protocol\": \"tcp\"\n"
      + "                }\n"
      + "    ], \n"
      + "    \"entryPoint\" : [ ],\n"
      + "    \"command\" : [ ],\n"
      + "    \"environment\" : [ ],\n"
      + "    \"mountPoints\" : [ ],\n"
      + "    \"volumesFrom\" : [ ],\n"
      + "    \"dnsServers\" : [ ],\n"
      + "    \"dnsSearchDomains\" : [ ],\n"
      + "    \"extraHosts\" : [ ],\n"
      + "    \"dockerSecurityOptions\" : [ ],\n"
      + "    \"ulimits\" : [ ]\n"
      + "  } ],\n"
      + "  \"volumes\" : [ ],\n"
      + "  \"requiresAttributes\" : [ ],\n"
      + "  \"placementConstraints\" : [ ],\n"
      + "  \"compatibilities\" : [ ],\n"
      + "  \"requiresCompatibilities\": [\n"
      + "        \"FARGATE\"\n"
      + "  ], \n"
      + "  \"cpu\": \"256\", \n"
      + "  \"memory\": \"1024\"\n"
      + "}";

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
  private CommandExecutionContext context = aCommandExecutionContext()
                                                .withCloudProviderSetting(computeProvider)
                                                .withContainerSetupParams(setupParams)
                                                .withCloudProviderCredentials(emptyList())
                                                .build();
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
  public void shouldExecuteWithLastService() {
    com.amazonaws.services.ecs.model.Service ecsService = new com.amazonaws.services.ecs.model.Service();
    ecsService.setServiceName(EcsConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision()));
    ecsService.setCreatedAt(new Date());

    when(awsClusterService.getServices(
             Regions.US_EAST_1.getName(), computeProvider, Collections.emptyList(), WingsTestConstants.CLUSTER_NAME))
        .thenReturn(Lists.newArrayList(ecsService));
    CommandExecutionStatus status = ecsSetupCommandUnit.execute(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(awsClusterService)
        .createTask(eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(),
            any(RegisterTaskDefinitionRequest.class));
    verify(awsClusterService)
        .createService(
            eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(), any(CreateServiceRequest.class));
  }

  @Test
  public void testIsFargateTaskLauchType() throws Exception {
    setupParams.setLaunchType(LaunchType.FARGATE.name());
    assertTrue((boolean) MethodUtils.invokeMethod(ecsSetupCommandUnit, true, "isFargateTaskLauchType", setupParams));

    setupParams.setLaunchType(LaunchType.EC2.name());
    assertFalse((boolean) MethodUtils.invokeMethod(ecsSetupCommandUnit, true, "isFargateTaskLauchType", setupParams));
  }

  @Test
  public void testIsValidateSetupParamasForECS() throws Exception {
    TaskDefinition taskDefinition = new TaskDefinition().withExecutionRoleArn("executionRole");

    EcsSetupParams ecsSetupParams = anEcsSetupParams()
                                        .withVpcId("vpc_id")
                                        .withSubnetIds(new String[] {"subnet_1"})
                                        .withSecurityGroupIds(new String[] {"sg_id"})
                                        .withExecutionRoleArn("executionRoleArn")
                                        .withLaunchType(LaunchType.FARGATE.name())
                                        .build();

    assertEquals(StringUtils.EMPTY,
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setSubnetIds(new String[] {"subnet_1", "subnet_2"});
    assertEquals(StringUtils.EMPTY,
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setVpcId(null);
    assertEquals("VPC Id is required for fargate task",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setVpcId("");
    assertEquals("VPC Id is required for fargate task",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setVpcId("vpc_id");
    ecsSetupParams.setSubnetIds(null);
    assertEquals("At least 1 subnetId is required for mentioned VPC",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setSubnetIds(new String[] {null});
    assertEquals("At least 1 subnetId is required for mentioned VPC",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setSubnetIds(new String[] {"subnet_id"});
    ecsSetupParams.setSecurityGroupIds(new String[0]);
    assertEquals("At least 1 security Group is required for mentioned VPC",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setSecurityGroupIds(null);
    assertEquals("At least 1 security Group is required for mentioned VPC",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setSecurityGroupIds(new String[] {null});
    assertEquals("At least 1 security Group is required for mentioned VPC",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    ecsSetupParams.setSecurityGroupIds(new String[] {"sg_id"});
    taskDefinition.setExecutionRoleArn(null);
    assertEquals("Execution Role ARN is required for Fargate tasks",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));

    taskDefinition.setExecutionRoleArn("");
    assertEquals("Execution Role ARN is required for Fargate tasks",
        (String) MethodUtils.invokeMethod(
            ecsSetupCommandUnit, true, "isValidateSetupParamasForECS", new Object[] {taskDefinition, ecsSetupParams}));
  }

  @Test
  public void testCreateEcsContainerTaskIfNull() throws Exception {
    EcsContainerTask ecsContainerTask = (EcsContainerTask) MethodUtils.invokeMethod(
        ecsSetupCommandUnit, true, "createEcsContainerTaskIfNull", new Object[] {null});

    assertNotNull(ecsContainerTask);
    assertNotNull(ecsContainerTask.getContainerDefinitions());
    ContainerDefinition containerDefinition = ecsContainerTask.getContainerDefinitions().iterator().next();
    assertEquals(1, ecsContainerTask.getContainerDefinitions().size());
    assertEquals(1, containerDefinition.getCpu().intValue());
    assertEquals(256, containerDefinition.getMemory().intValue());
    assertNotNull(containerDefinition.getPortMappings());
    assertEquals(0, containerDefinition.getPortMappings().size());
    assertNull(containerDefinition.getLogConfiguration());
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
        (CreateServiceRequest) MethodUtils.invokeMethod(ecsSetupCommandUnit, true, "getCreateServiceRequest",
            new Object[] {computeProvider, encryptedDataDetails, setupParams, taskDefinition, CONTAINER_SERVICE_NAME,
                executionLogCallback});

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
        (CreateServiceRequest) MethodUtils.invokeMethod(ecsSetupCommandUnit, true, "getCreateServiceRequest",
            new Object[] {computeProvider, encryptedDataDetails, setupParams, taskDefinition, CONTAINER_SERVICE_NAME,
                executionLogCallback});

    assertCreateServiceRequestObject(taskDefinition, createServiceRequest);
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

  @NotNull
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
        (CreateServiceRequest) MethodUtils.invokeMethod(ecsSetupCommandUnit, true, "getCreateServiceRequest",
            new Object[] {computeProvider, encryptedDataDetails, setupParams, taskDefinition, CONTAINER_SERVICE_NAME,
                executionLogCallback});

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

  @Test
  public void testCreateTaskDefinition() throws Exception {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(fargateConfigYaml);

    EcsSetupParams setupParams = anEcsSetupParams()
                                     .withClusterName(CLUSTER_NAME)
                                     .withTargetGroupArn(TARGET_GROUP_ARN)
                                     .withRoleArn(ROLE_ARN)
                                     .withAssignPublicIps(true)
                                     .withVpcId(VPC_ID)
                                     .withSecurityGroupIds(new String[] {SECURITY_GROUP_ID_1})
                                     .withSubnetIds(new String[] {SUBNET_ID})
                                     .withLaunchType(LaunchType.FARGATE.name())
                                     .withExecutionRoleArn("arn")
                                     .withUseLoadBalancer(true)
                                     .withTaskFamily(TASK_FAMILY)
                                     .withRegion(Regions.US_EAST_1.name())
                                     .build();

    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    SettingAttribute settingAttribute = new SettingAttribute();

    TaskDefinition taskDefinition =
        (TaskDefinition) MethodUtils.invokeMethod(ecsSetupCommandUnit, true, "createTaskDefinition",
            new Object[] {ecsContainerTask, CONTAINER_NAME, DOCKER_IMG_NAME, setupParams, settingAttribute,
                new HashMap<>(), new HashMap<>(), encryptedDataDetails, executionLogCallback, DOCKER_DOMAIN_NAME});

    // Capture RegisterTaskDefinitionRequest arg that was passed to "awsClusterService.createTask" and assert it
    ArgumentCaptor<RegisterTaskDefinitionRequest> captor = ArgumentCaptor.forClass(RegisterTaskDefinitionRequest.class);
    verify(awsClusterService, times(1)).createTask(anyObject(), anyObject(), anyObject(), captor.capture());
    RegisterTaskDefinitionRequest registerTaskDefinitionRequest = captor.getValue();

    assertNotNull(registerTaskDefinitionRequest);
    assertEquals("256", registerTaskDefinitionRequest.getCpu());
    assertEquals("1024", registerTaskDefinitionRequest.getMemory());
    assertEquals(
        "arn:aws:iam::830767422336:role/ecsTaskExecutionRole", registerTaskDefinitionRequest.getExecutionRoleArn());
    assertEquals(setupParams.getTaskFamily(), registerTaskDefinitionRequest.getFamily());
    assertTrue(registerTaskDefinitionRequest.getRequiresCompatibilities().contains(LaunchType.FARGATE.name()));
    assertEquals(NetworkMode.Awsvpc.name().toLowerCase(), registerTaskDefinitionRequest.getNetworkMode().toLowerCase());
    assertEquals(1, registerTaskDefinitionRequest.getContainerDefinitions().size());
    com.amazonaws.services.ecs.model.ContainerDefinition taskDefinition1 =
        registerTaskDefinitionRequest.getContainerDefinitions().iterator().next();
    assertNotNull(taskDefinition1.getPortMappings());
    assertEquals(1, taskDefinition1.getPortMappings().size());

    PortMapping portMapping = taskDefinition1.getPortMappings().iterator().next();
    assertEquals("tcp", portMapping.getProtocol());
    assertEquals(80, portMapping.getContainerPort().intValue());
  }
}
