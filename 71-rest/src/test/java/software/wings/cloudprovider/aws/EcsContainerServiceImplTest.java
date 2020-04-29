package software.wings.cloudprovider.aws;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.impl.aws.model.AwsConstants.MAIN_ECS_CONTAINER_NAME_TAG;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.AUTO_SCALING_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.LAUNCHER_TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.Container;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.Deployment;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.NetworkInterface;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceEvent;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.service.impl.AwsHelperService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 1/3/17.
 */
public class EcsContainerServiceImplTest extends WingsBaseTest {
  @Mock private AwsHelperService awsHelperService;
  @Inject @InjectMocks private EcsContainerService ecsContainerService;

  private SettingAttribute connectorConfig =
      aSettingAttribute().withValue(AwsConfig.builder().accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build()).build();

  private AwsConfig awsConfig = (AwsConfig) connectorConfig.getValue();

  private static final int DESIRED_COUNT = 2;

  @Before
  public void setUp() throws Exception {
    when(awsHelperService.validateAndGetAwsConfig(any(SettingAttribute.class), anyObject()))
        .thenReturn((AwsConfig) connectorConfig.getValue());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldCreadAutoScalingGroupAndProvisionNodes() {
    DescribeAutoScalingGroupsResult autoScalingGroupsResult =
        new DescribeAutoScalingGroupsResult().withAutoScalingGroups(new AutoScalingGroup().withInstances(
            asList(new Instance().withLifecycleState("InService"), new Instance().withLifecycleState("InService"))));

    DescribeClustersResult describeClustersResult = new DescribeClustersResult().withClusters(
        asList(new Cluster().withClusterName(CLUSTER_NAME).withRegisteredContainerInstancesCount(2)));
    when(awsHelperService.describeAutoScalingGroups(awsConfig, Collections.emptyList(), Regions.US_EAST_1.getName(),
             new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asList(AUTO_SCALING_GROUP_NAME))))
        .thenReturn(autoScalingGroupsResult);

    Map<String, Object> params = new HashMap<>();
    params.put("autoScalingGroupName", AUTO_SCALING_GROUP_NAME);
    params.put("clusterName", CLUSTER_NAME);
    params.put("availabilityZones", asList("AZ1", "AZ2"));
    params.put("vpcZoneIdentifiers", "VPC_ZONE_1, VPC_ZONE_2");

    when(awsHelperService.describeClusters(Regions.US_EAST_1.getName(), awsConfig, Collections.emptyList(),
             new DescribeClustersRequest().withClusters(CLUSTER_NAME)))
        .thenReturn(describeClustersResult);
    ecsContainerService.provisionNodes(Regions.US_EAST_1.getName(), connectorConfig, Collections.emptyList(),
        DESIRED_COUNT, LAUNCHER_TEMPLATE_NAME, params, null);

    verify(awsHelperService)
        .createAutoScalingGroup(awsConfig, Collections.emptyList(), Regions.US_EAST_1.getName(),
            new CreateAutoScalingGroupRequest()
                .withLaunchConfigurationName(LAUNCHER_TEMPLATE_NAME)
                .withDesiredCapacity(DESIRED_COUNT)
                .withMaxSize(2 * DESIRED_COUNT)
                .withMinSize(DESIRED_COUNT / 2)
                .withAutoScalingGroupName(AUTO_SCALING_GROUP_NAME)
                .withAvailabilityZones(asList("AZ1", "AZ2"))
                .withVPCZoneIdentifier("VPC_ZONE_1, VPC_ZONE_2"),
            null);
    verify(awsHelperService)
        .describeAutoScalingGroups(awsConfig, Collections.emptyList(), Regions.US_EAST_1.getName(),
            new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asList(AUTO_SCALING_GROUP_NAME)));
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldDeployService() {
    String serviceJson =
        "{\"cluster\":\"CLUSTER_NAME\",\"desiredCount\":\"2\",\"serviceName\":\"SERVICE_NAME\",\"taskDefinition\":\"TASK_TEMPLATE\"}";
    CreateServiceRequest createServiceRequest = new CreateServiceRequest()
                                                    .withCluster(CLUSTER_NAME)
                                                    .withServiceName(SERVICE_NAME)
                                                    .withTaskDefinition("TASK_TEMPLATE")
                                                    .withDesiredCount(DESIRED_COUNT);
    Service service =
        new Service().withDesiredCount(DESIRED_COUNT).withRunningCount(DESIRED_COUNT).withServiceArn("SERVICE_ARN");
    when(awsHelperService.describeServices(anyString(), any(AwsConfig.class), anyObject(), any()))
        .thenReturn(new DescribeServicesResult().withServices(asList(service)));

    when(awsHelperService.createService(
             Regions.US_EAST_1.getName(), awsConfig, Collections.emptyList(), createServiceRequest))
        .thenReturn(new CreateServiceResult().withService(service));
    String serviceArn = ecsContainerService.deployService(
        Regions.US_EAST_1.getName(), connectorConfig, Collections.emptyList(), serviceJson);

    verify(awsHelperService)
        .createService(Regions.US_EAST_1.getName(), awsConfig, Collections.emptyList(), createServiceRequest);
    assertThat(serviceArn).isEqualTo("SERVICE_ARN");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldDeleteService() {
    ecsContainerService.deleteService(
        Regions.US_EAST_1.getName(), connectorConfig, Collections.emptyList(), CLUSTER_NAME, SERVICE_NAME);
    verify(awsHelperService)
        .deleteService(Regions.US_EAST_1.getName(), (AwsConfig) connectorConfig.getValue(), Collections.emptyList(),
            new DeleteServiceRequest().withCluster(CLUSTER_NAME).withService(SERVICE_NAME));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldProvisionTasks() {
    when(awsHelperService.describeServices(anyString(), any(AwsConfig.class), any(), any()))
        .thenReturn(new DescribeServicesResult().withServices(
            asList(new Service().withDesiredCount(DESIRED_COUNT).withRunningCount(DESIRED_COUNT))));
    when(awsHelperService.describeTasks(anyString(), any(AwsConfig.class), any(), any()))
        .thenReturn(new DescribeTasksResult());
    ecsContainerService.provisionTasks(Regions.US_EAST_1.getName(), connectorConfig, Collections.emptyList(),
        CLUSTER_NAME, SERVICE_NAME, 0, DESIRED_COUNT, 10, new ExecutionLogCallback());
    verify(awsHelperService)
        .updateService(Regions.US_EAST_1.getName(), awsConfig, Collections.emptyList(),
            new UpdateServiceRequest()
                .withCluster(CLUSTER_NAME)
                .withService(SERVICE_NAME)
                .withDesiredCount(DESIRED_COUNT));
    verify(awsHelperService).describeTasks(anyString(), any(AwsConfig.class), any(), any(DescribeTasksRequest.class));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetIdFromArn() {
    EcsContainerServiceImpl ecsContainerServiceImpl = (EcsContainerServiceImpl) ecsContainerService;
    assertThat(ecsContainerServiceImpl.getIdFromArn(
                   "arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/b506302e5cf6448ca67e1896b679c92e"))
        .isEqualTo("b506302e5cf6448ca67e1896b679c92e");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHasServiceReachedSteadyState() throws Exception {
    EcsContainerServiceImpl ecsContainerServiceImpl = (EcsContainerServiceImpl) ecsContainerService;

    Service service = new Service().withDeployments(new Deployment(), new Deployment());
    boolean ret = ecsContainerServiceImpl.hasServiceReachedSteadyState(service);

    assertThat(ret).isFalse();

    service =
        new Service()
            .withDeployments(new Deployment().withUpdatedAt(new Date(10)))
            .withEvents(new ServiceEvent().withMessage("Foo has reached a steady state.").withCreatedAt(new Date(5)));
    ret = ecsContainerServiceImpl.hasServiceReachedSteadyState(service);
    assertThat(ret).isFalse();

    service =
        new Service()
            .withDeployments(new Deployment().withUpdatedAt(new Date(10)))
            .withEvents(new ServiceEvent().withMessage("Foo has reached a steady state.").withCreatedAt(new Date(15)));
    ret = ecsContainerServiceImpl.hasServiceReachedSteadyState(service);
    assertThat(ret).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAwsMetadataApiHelper() throws Exception {
    String json =
        "{\"Tasks\":[{\"Arn\":\"arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/f0c1d86cfa154d36b4c67b8ec72fda6d\",\"DesiredStatus\":\"STOPPED\",\"KnownStatus\":\"STOPPED\",\"Family\":\"AWS__ECS__awsvpc__Ecs__Awsvpc__mode__Test\",\"Version\":\"26\",\"Containers\":[{\"DockerId\":\"b8013be505613d216cbeaa911f8c4ac013d5522cfc012e427379b617196f8a42\",\"DockerName\":\"ecs-AWS__ECS__awsvpc__Ecs__Awsvpc__mode__Test-26-448640225317dkrecrus-east-1amazonawscomhello-worldlatest-f4c4fbe8fff4e8f7e701\",\"Name\":\"448640225317_dkr_ecr_us-east-1_amazonaws_com_hello-world_latest\"}]},{\"Arn\":\"arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/bc26c8dffd0446009fb6f41ee4298298\",\"DesiredStatus\":\"RUNNING\",\"KnownStatus\":\"RUNNING\",\"Family\":\"AWS__ECS__awsvpc__Ecs__Awsvpc__mode__Test\",\"Version\":\"28\",\"Containers\":[{\"DockerId\":\"f40291c50dd71caa7b39e13f3471059906e92a52b5078e76718dadfb0f5009d3\",\"DockerName\":\"ecs-AWS__ECS__awsvpc__Ecs__Awsvpc__mode__Test-28-448640225317dkrecrus-east-1amazonawscomhello-worldlatest-aebadbbd98f9bf954700\",\"Name\":\"448640225317_dkr_ecr_us-east-1_amazonaws_com_hello-world_latest\"}]},{\"Arn\":\"arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/e0a96879647145bc81dd3d5ca482dd2a\",\"DesiredStatus\":\"STOPPED\",\"KnownStatus\":\"STOPPED\",\"Family\":\"AWS__ECS__awsvpc__Ecs__Awsvpc__mode__Test\",\"Version\":\"25\",\"Containers\":[{\"DockerId\":\"e7871c6f03f0c4bec48e18182ac33f134ebcbef0c89fcc3ff82da769b2be6fa8\",\"DockerName\":\"ecs-AWS__ECS__awsvpc__Ecs__Awsvpc__mode__Test-25-448640225317dkrecrus-east-1amazonawscomhello-worldlatest-a4edd0fdad9afcff1f00\",\"Name\":\"448640225317_dkr_ecr_us-east-1_amazonaws_com_hello-world_latest\",\"Networks\":[{\"NetworkMode\":\"awsvpc\",\"IPv4Addresses\":[\"172.31.21.197\"]}]}]}]}";

    TaskMetadata metadata = JsonUtils.asObject(json, TaskMetadata.class);
    TaskMetadata.Task task =
        metadata.getTasks()
            .stream()
            .filter(task1
                -> task1.getArn().equals(
                    "arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/e0a96879647145bc81dd3d5ca482dd2a"))
            .findFirst()
            .get();

    assertThat(task).isNotNull();
    assertThat(task.getContainers()).isNotNull();
    assertThat(task.getContainers().get(0).getNetworks()).isNotNull();

    TaskMetadata.Network network = task.getContainers().get(0).getNetworks().get(0);
    assertThat(network.getIPv4Addresses()).isNotNull();
    assertThat(network.getIPv4Addresses().get(0)).isEqualTo("172.31.21.197");

    AwsMetadataApiHelper awsMetadataApiHelper = spy(AwsMetadataApiHelper.class);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString(), any());
    doReturn(json).when(awsMetadataApiHelper).getResponseStringFromUrl(anyString());
    doReturn(false).doReturn(true).when(awsMetadataApiHelper).checkConnectivity(anyString());
    com.amazonaws.services.ec2.model.Instance ec2Instance =
        new com.amazonaws.services.ec2.model.Instance().withPrivateIpAddress("2.0.0.0");
    String dockerId = awsMetadataApiHelper.getDockerIdUsingEc2MetadataEndpointApi(ec2Instance,
        new Task().withTaskArn("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/bc26c8dffd0446009fb6f41ee4298298"),
        "448640225317_dkr_ecr_us-east-1_amazonaws_com_hello-world_latest", logCallback);
    assertThat(dockerId).isEqualTo("f40291c50dd71caa7b39e13f3471059906e92a52b5078e76718dadfb0f5009d3");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testgetEcsContainerDetailsBuilder() {
    Task task = mock(Task.class);
    doReturn("abc/taskId").when(task).getTaskArn();
    Container container = mock(Container.class);
    doReturn("abcdefghijkl123456").when(container).getRuntimeId();

    EcsContainerDetails ecsContainerDetails =
        ((EcsContainerServiceImpl) ecsContainerService).getEcsContainerDetailsBuilder(task, container).build();
    assertThat(ecsContainerDetails).isNotNull();

    assertThat(ecsContainerDetails.getDockerId()).isEqualTo("abcdefghijkl");
    assertThat(ecsContainerDetails.getTaskArn()).isEqualTo("abc/taskId");
    assertThat(ecsContainerDetails.getTaskId()).isEqualTo("taskId");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetMainHarnessDeployedContainer() {
    Task task = mock(Task.class);
    Container container = new Container();
    container.setName("containerMain");
    Container containerSideCar = new Container();
    containerSideCar.setName("sidecar");

    doReturn(Arrays.asList(container)).when(task).getContainers();
    Container mainHarnessDeployedContainer =
        ((EcsContainerServiceImpl) ecsContainerService).getMainHarnessDeployedContainer(task, "us-east-1", null, null);
    assertThat(mainHarnessDeployedContainer).isEqualTo(container);

    com.amazonaws.services.ecs.model.Tag tag =
        new com.amazonaws.services.ecs.model.Tag().withKey(MAIN_ECS_CONTAINER_NAME_TAG).withValue("containerMain");

    DescribeTaskDefinitionResult describeTaskDefinitionResult = new DescribeTaskDefinitionResult().withTags(tag);
    doReturn(Arrays.asList(containerSideCar, container)).when(task).getContainers();
    doReturn(describeTaskDefinitionResult)
        .when(awsHelperService)
        .describeTaskDefinition(anyString(), any(), anyList(), any());

    mainHarnessDeployedContainer =
        ((EcsContainerServiceImpl) ecsContainerService).getMainHarnessDeployedContainer(task, "us", null, null);
    assertThat(mainHarnessDeployedContainer).isEqualTo(container);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testInitEcsContainerDetailsBuilder() {
    Task task = new Task();
    task.setContainerInstanceArn(
        "arn:aws:ecs:us-east-1:448640225317:containerInstance/SdkTesting/b506302e5cf6448ca67e1896b679c92e");
    task.setTaskArn("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/c506302e5cf6448ca67e1896b679c92e");

    Container container = new Container();
    container.setContainerArn(
        "arn:aws:ecs:us-east-1:448640225317:container/SdkTesting/d506302e5cf6448ca67e1896b679c92e");
    container.setRuntimeId("123456789abcdefghijklmnopqrstu");

    EcsContainerDetails ecsContainerDetails =
        ((EcsContainerServiceImpl) ecsContainerService).initEcsContainerDetailsBuilder(task, container).build();

    assertThat(ecsContainerDetails.getTaskArn())
        .isEqualTo("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/c506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getTaskId()).isEqualTo("c506302e5cf6448ca67e1896b679c92e");

    assertThat(ecsContainerDetails.getContainerInstanceArn())
        .isEqualTo("arn:aws:ecs:us-east-1:448640225317:containerInstance/SdkTesting/b506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getContainerInstanceId()).isEqualTo("b506302e5cf6448ca67e1896b679c92e");

    assertThat(ecsContainerDetails.getContainerId()).isEqualTo("d506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getDockerId()).isEqualTo("123456789abc");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateContainerInfos_Fargate() {
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doNothing().when(logCallback).saveExecutionLog(anyString(), any(), any());
    // Main Container
    Container container = new Container();
    container.setName("containerMain");
    container.setContainerArn(
        "arn:aws:ecs:us-east-1:448640225317:container/SdkTesting/d506302e5cf6448ca67e1896b679c92e");
    container.setRuntimeId("123456789abcdefghijklmnopqrstu");

    NetworkInterface networkInterface = new NetworkInterface().withPrivateIpv4Address("1.0.0.1");
    container.setNetworkInterfaces(Arrays.asList(networkInterface));

    // Sidecar container
    Container containerSideCar = new Container();
    containerSideCar.setName("sidecar");

    // Task
    Task task = new Task();
    task.setLaunchType(LaunchType.FARGATE.name());
    task.setTaskArn("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/c506302e5cf6448ca67e1896b679c92e");
    task.setContainers(Arrays.asList(container, containerSideCar));

    com.amazonaws.services.ecs.model.Tag tag =
        new com.amazonaws.services.ecs.model.Tag().withKey(MAIN_ECS_CONTAINER_NAME_TAG).withValue("containerMain");
    DescribeTaskDefinitionResult describeTaskDefinitionResult = new DescribeTaskDefinitionResult().withTags(tag);
    doReturn(describeTaskDefinitionResult)
        .when(awsHelperService)
        .describeTaskDefinition(anyString(), any(), anyList(), any());

    List<ContainerInfo> containerInfos = ((EcsContainerServiceImpl) ecsContainerService)
                                             .generateContainerInfos(Arrays.asList(task), "cl1", "us-east-1", null,
                                                 logCallback, AwsConfig.builder().build(), null, Arrays.asList("abc"));

    assertThat(containerInfos).isNotEmpty();
    assertThat(containerInfos.size()).isEqualTo(1);

    ContainerInfo containerInfo = containerInfos.get(0);
    assertThat(containerInfo.isNewContainer()).isTrue();

    EcsContainerDetails ecsContainerDetails = containerInfo.getEcsContainerDetails();
    assertThat(ecsContainerDetails.getTaskArn())
        .isEqualTo("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/c506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getTaskId()).isEqualTo("c506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getDockerId()).isEqualTo("123456789abc");

    assertThat(containerInfo.getHostName()).isEqualTo("123456789abc");
    assertThat(containerInfo.getIp()).isEqualTo("1.0.0.1");
    assertThat(containerInfo.getContainerId()).isEqualTo("123456789abc");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateContainerInfos_ECS_EC2() {
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doNothing().when(logCallback).saveExecutionLog(anyString(), any(), any());
    // Main Container
    Container container = generateMainContainer();
    // Sidecar container
    Container containerSideCar = new Container();
    containerSideCar.setName("sidecar");

    // Task
    String containerInstanceArn =
        "arn:aws:ecs:us-east-1:448640225317:containerInstance/SdkTesting/c506302e5cf6448ca67e1896b679c92e";
    ContainerInstance containerInstance =
        new ContainerInstance().withContainerInstanceArn(containerInstanceArn).withEc2InstanceId("ec2Id");
    Task task = generateTask(container, containerSideCar, containerInstanceArn);

    DescribeContainerInstancesResult containerInstancesResult =
        new DescribeContainerInstancesResult().withContainerInstances(containerInstance);
    doReturn(containerInstancesResult)
        .when(awsHelperService)
        .describeContainerInstances(anyString(), any(), anyList(), any());

    com.amazonaws.services.ec2.model.Instance ec2Instance =
        new com.amazonaws.services.ec2.model.Instance().withPrivateIpAddress("2.0.0.0");
    DescribeInstancesResult describeInstancesResult =
        new DescribeInstancesResult().withReservations(new Reservation().withInstances(ec2Instance));
    doReturn(describeInstancesResult).when(awsHelperService).describeEc2Instances(any(), anyList(), anyString(), any());

    com.amazonaws.services.ecs.model.Tag tag =
        new com.amazonaws.services.ecs.model.Tag().withKey(MAIN_ECS_CONTAINER_NAME_TAG).withValue("containerMain");
    DescribeTaskDefinitionResult describeTaskDefinitionResult = new DescribeTaskDefinitionResult().withTags(tag);
    doReturn(describeTaskDefinitionResult)
        .when(awsHelperService)
        .describeTaskDefinition(anyString(), any(), anyList(), any());

    List<ContainerInfo> containerInfos = ((EcsContainerServiceImpl) ecsContainerService)
                                             .generateContainerInfos(Arrays.asList(task), "cl1", "us-east-1", null,
                                                 logCallback, AwsConfig.builder().build(), null, Arrays.asList("abc"));

    assertThat(containerInfos).isNotEmpty();
    assertThat(containerInfos.size()).isEqualTo(1);

    ContainerInfo containerInfo = containerInfos.get(0);
    assertThat(containerInfo.isNewContainer()).isTrue();

    EcsContainerDetails ecsContainerDetails = containerInfo.getEcsContainerDetails();
    assertThat(ecsContainerDetails.getTaskArn())
        .isEqualTo("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/c506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getTaskId()).isEqualTo("c506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getDockerId()).isEqualTo("123456789abc");

    assertThat(containerInfo.getHostName()).isEqualTo("123456789abc");
    assertThat(containerInfo.getIp()).isEqualTo("2.0.0.0");
    assertThat(containerInfo.getContainerId()).isEqualTo("123456789abc");
    assertThat(containerInfo.getEc2Instance()).isEqualTo(ec2Instance);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateContainerInfos_ECS_EC2_AWSVPC() {
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doNothing().when(logCallback).saveExecutionLog(anyString(), any(), any());
    // Main Container
    Container container = generateMainContainer();
    NetworkInterface networkInterface = new NetworkInterface().withPrivateIpv4Address("1.0.0.1");
    container.setNetworkInterfaces(Arrays.asList(networkInterface));
    // Sidecar container
    Container containerSideCar = new Container();
    containerSideCar.setName("sidecar");

    // Task
    String containerInstanceArn =
        "arn:aws:ecs:us-east-1:448640225317:containerInstance/SdkTesting/c506302e5cf6448ca67e1896b679c92e";
    ContainerInstance containerInstance =
        new ContainerInstance().withContainerInstanceArn(containerInstanceArn).withEc2InstanceId("ec2Id");
    Task task = generateTask(container, containerSideCar, containerInstanceArn);

    DescribeContainerInstancesResult containerInstancesResult =
        new DescribeContainerInstancesResult().withContainerInstances(containerInstance);
    doReturn(containerInstancesResult)
        .when(awsHelperService)
        .describeContainerInstances(anyString(), any(), anyList(), any());

    com.amazonaws.services.ec2.model.Instance ec2Instance =
        new com.amazonaws.services.ec2.model.Instance().withPrivateIpAddress("2.0.0.0");
    DescribeInstancesResult describeInstancesResult =
        new DescribeInstancesResult().withReservations(new Reservation().withInstances(ec2Instance));
    doReturn(describeInstancesResult).when(awsHelperService).describeEc2Instances(any(), anyList(), anyString(), any());

    com.amazonaws.services.ecs.model.Tag tag =
        new com.amazonaws.services.ecs.model.Tag().withKey(MAIN_ECS_CONTAINER_NAME_TAG).withValue("containerMain");
    DescribeTaskDefinitionResult describeTaskDefinitionResult = new DescribeTaskDefinitionResult().withTags(tag);
    doReturn(describeTaskDefinitionResult)
        .when(awsHelperService)
        .describeTaskDefinition(anyString(), any(), anyList(), any());

    List<ContainerInfo> containerInfos = ((EcsContainerServiceImpl) ecsContainerService)
                                             .generateContainerInfos(Arrays.asList(task), "cl1", "us-east-1", null,
                                                 logCallback, AwsConfig.builder().build(), null, Arrays.asList("abc"));

    assertThat(containerInfos).isNotEmpty();
    assertThat(containerInfos.size()).isEqualTo(1);

    ContainerInfo containerInfo = containerInfos.get(0);
    assertThat(containerInfo.isNewContainer()).isTrue();

    EcsContainerDetails ecsContainerDetails = containerInfo.getEcsContainerDetails();
    assertThat(ecsContainerDetails.getTaskArn())
        .isEqualTo("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/c506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getTaskId()).isEqualTo("c506302e5cf6448ca67e1896b679c92e");
    assertThat(ecsContainerDetails.getDockerId()).isEqualTo("123456789abc");

    assertThat(containerInfo.getHostName()).isEqualTo("123456789abc");
    assertThat(containerInfo.getIp()).isEqualTo("1.0.0.1");
    assertThat(containerInfo.getContainerId()).isEqualTo("123456789abc");
    assertThat(containerInfo.getEc2Instance()).isEqualTo(ec2Instance);
  }

  private Task generateTask(Container container, Container containerSideCar, String containerInstanceArn) {
    Task task = new Task();
    task.setLaunchType(LaunchType.EC2.name());
    task.setTaskArn("arn:aws:ecs:us-east-1:448640225317:task/SdkTesting/c506302e5cf6448ca67e1896b679c92e");
    task.setContainerInstanceArn(containerInstanceArn);
    task.setContainers(Arrays.asList(container, containerSideCar));
    return task;
  }

  private Container generateMainContainer() {
    Container container = new Container();
    container.setName("containerMain");
    container.setContainerArn(
        "arn:aws:ecs:us-east-1:448640225317:container/SdkTesting/d506302e5cf6448ca67e1896b679c92e");
    container.setRuntimeId("123456789abcdefghijklmnopqrstu");
    return container;
  }
}
