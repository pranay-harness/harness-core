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
import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.NetworkMode;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  @InjectMocks @Inject private EcsSetupCommandUnit ecsSetupCommandUnit;
  @Inject private EcsCommandUnitHelper ecsCommandUnitHelper;
  private static Logger logger = LoggerFactory.getLogger(EcsSetupCommandUnitTest.class);

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

  private static final String taskDefinitionYaml = "{\n"
      + "  \"executionRoleArn\": \"arn:aws:iam::733017110293:role/ecsTaskExecutionRole\",\n"
      + "  \"containerDefinitions\": [\n"
      + "    {\n"
      + "      \"entryPoint\": null,\n"
      + "      \"portMappings\": [\n"
      + "        {\n"
      + "          \"protocol\": \"tcp\",\n"
      + "          \"containerPort\": 80\n"
      + "        }\n      ],\n"
      + "      \"cpu\": 1000,\n"
      + "\"repositoryCredentials\": {\n"
      + "        \"credentialsParameter\": \"arn:aws:secretsmanager:us-west-2:625734649295:secret:dockerhub/marlinadmin-ktQyEA\"\n      },\n"
      + "      \"memory\": 2800,\n"
      + "      \"memoryReservation\": null,\n"
      + "      \"volumesFrom\": [],\n"
      + "      \"image\": \"${DOCKER_IMAGE_NAME}\",\n"
      + "      \"dockerLabels\": null,\n"
      + "      \"privileged\": null,\n"
      + "      \"name\": \"${CONTAINER_NAME}\"\n"
      + "    }\n"
      + "  ],\n"
      + "  \"placementConstraints\": [],\n"
      + "  \"memory\": null,\n"
      + "  \"taskRoleArn\": null,\n"
      + "  \"compatibilities\": [\n"
      + "    \"EC2\"\n"
      + "  ],\n"
      + " \"networkMode\": \"awsvpc\",\n"
      + "  \"requiresCompatibilities\": [],\n"
      + "  \"cpu\": null,\n"
      + "  \"revision\": 77,\n"
      + "  \"status\": \"ACTIVE\",\n"
      + "  \"volumes\": []\n"
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
    assertTrue(ecsCommandUnitHelper.isFargateTaskLauchType(setupParams));

    setupParams.setLaunchType(LaunchType.EC2.name());
    assertFalse(ecsCommandUnitHelper.isFargateTaskLauchType(setupParams));
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

    assertEquals(StringUtils.EMPTY, ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setSubnetIds(new String[] {"subnet_1", "subnet_2"});
    assertEquals(StringUtils.EMPTY, ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setVpcId(null);
    assertEquals("VPC Id is required for fargate task",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setVpcId("");
    assertEquals("VPC Id is required for fargate task",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setVpcId("vpc_id");
    ecsSetupParams.setSubnetIds(null);
    assertEquals("At least 1 subnetId is required for mentioned VPC",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setSubnetIds(new String[] {null});
    assertEquals("At least 1 subnetId is required for mentioned VPC",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setSubnetIds(new String[] {"subnet_id"});
    ecsSetupParams.setSecurityGroupIds(new String[0]);
    assertEquals("At least 1 security Group is required for mentioned VPC",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setSecurityGroupIds(null);
    assertEquals("At least 1 security Group is required for mentioned VPC",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setSecurityGroupIds(new String[] {null});
    assertEquals("At least 1 security Group is required for mentioned VPC",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    ecsSetupParams.setSecurityGroupIds(new String[] {"sg_id"});
    taskDefinition.setExecutionRoleArn(null);
    assertEquals("Execution Role ARN is required for Fargate tasks",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));

    taskDefinition.setExecutionRoleArn("");
    assertEquals("Execution Role ARN is required for Fargate tasks",
        ecsCommandUnitHelper.isValidateSetupParamasForECS(taskDefinition, ecsSetupParams));
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
  public void testCreateTaskDefinition_Fargate() throws Exception {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(fargateConfigYaml);

    EcsSetupParams setupParams = generateEcsSetupParams(LaunchType.FARGATE);

    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    SettingAttribute settingAttribute = new SettingAttribute();

    TaskDefinition taskDefinition = ecsCommandUnitHelper.createTaskDefinition(ecsContainerTask, CONTAINER_NAME,
        DOCKER_IMG_NAME, setupParams, settingAttribute, new HashMap<>(), new HashMap<>(), encryptedDataDetails,
        executionLogCallback, DOCKER_DOMAIN_NAME, awsClusterService);

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

  @Test
  public void testCreateTaskDefinition_ECS() throws Exception {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(taskDefinitionYaml);

    EcsSetupParams setupParams = generateEcsSetupParams(LaunchType.EC2);

    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    SettingAttribute settingAttribute = new SettingAttribute();

    TaskDefinition taskDefinition = ecsCommandUnitHelper.createTaskDefinition(ecsContainerTask, CONTAINER_NAME,
        DOCKER_IMG_NAME, setupParams, settingAttribute, new HashMap<>(), new HashMap<>(), encryptedDataDetails,
        executionLogCallback, DOCKER_DOMAIN_NAME, awsClusterService);

    // Capture RegisterTaskDefinitionRequest arg that was passed to "awsClusterService.createTask" and assert it
    ArgumentCaptor<RegisterTaskDefinitionRequest> captorArg =
        ArgumentCaptor.forClass(RegisterTaskDefinitionRequest.class);
    verify(awsClusterService, times(1)).createTask(anyObject(), anyObject(), anyObject(), captorArg.capture());
    RegisterTaskDefinitionRequest registerTaskDefinitionRequest = captorArg.getValue();

    assertNotNull(registerTaskDefinitionRequest);
    assertNotNull(registerTaskDefinitionRequest.getContainerDefinitions());
    assertEquals(1, registerTaskDefinitionRequest.getContainerDefinitions().size());
    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinition =
        registerTaskDefinitionRequest.getContainerDefinitions().get(0);

    assertEquals(1000, containerDefinition.getCpu().intValue());
    assertEquals(2800, containerDefinition.getMemory().intValue());
    assertEquals(
        "arn:aws:iam::733017110293:role/ecsTaskExecutionRole", registerTaskDefinitionRequest.getExecutionRoleArn());

    assertEquals(setupParams.getTaskFamily(), registerTaskDefinitionRequest.getFamily());
    assertEquals(NetworkMode.Awsvpc.name().toLowerCase(), registerTaskDefinitionRequest.getNetworkMode().toLowerCase());
    assertNotNull(containerDefinition.getPortMappings());
    assertEquals(1, containerDefinition.getPortMappings().size());

    PortMapping portMapping = containerDefinition.getPortMappings().iterator().next();
    assertEquals(80, portMapping.getContainerPort().intValue());
    assertEquals("tcp", portMapping.getProtocol());
  }

  private EcsSetupParams generateEcsSetupParams(LaunchType launchType) {
    return anEcsSetupParams()
        .withClusterName(CLUSTER_NAME)
        .withTargetGroupArn(TARGET_GROUP_ARN)
        .withRoleArn(ROLE_ARN)
        .withAssignPublicIps(true)
        .withVpcId(VPC_ID)
        .withSecurityGroupIds(new String[] {SECURITY_GROUP_ID_1})
        .withSubnetIds(new String[] {SUBNET_ID})
        .withLaunchType(launchType.name())
        .withExecutionRoleArn("arn")
        .withUseLoadBalancer(true)
        .withTaskFamily(TASK_FAMILY)
        .withRegion(Regions.US_EAST_1.name())
        .build();
  }
}
