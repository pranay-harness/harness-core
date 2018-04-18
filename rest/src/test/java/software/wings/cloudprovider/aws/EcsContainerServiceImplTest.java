package software.wings.cloudprovider.aws;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.AUTO_SCALING_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.LAUNCHER_TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceEvent;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.AwsHelperService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
  public void shouldDeleteService() {
    ecsContainerService.deleteService(
        Regions.US_EAST_1.getName(), connectorConfig, Collections.emptyList(), CLUSTER_NAME, SERVICE_NAME);
    verify(awsHelperService)
        .deleteService(Regions.US_EAST_1.getName(), (AwsConfig) connectorConfig.getValue(), Collections.emptyList(),
            new DeleteServiceRequest().withCluster(CLUSTER_NAME).withService(SERVICE_NAME));
  }

  @Test
  @Ignore // TODO:: remove ignore
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
  public void testWaitForServiceToReachSteadyState() throws Exception {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString(), any());

    EcsContainerServiceImpl ecsContainerServiceImpl = (EcsContainerServiceImpl) ecsContainerService;

    final OffsetDateTime startDate = OffsetDateTime.of(2018, 02, 02, 02, 10, 0, 0, ZoneOffset.UTC);

    List<ServiceEvent> events = new ArrayList<>();
    events.add(new ServiceEvent()
                   .withCreatedAt(Date.from(startDate.plusMinutes(0).toInstant()))
                   .withId("E1")
                   .withMessage("message"));
    events.add(new ServiceEvent()
                   .withCreatedAt(Date.from(startDate.plusMinutes(7).toInstant()))
                   .withId("E3")
                   .withMessage("message"));
    events.add(new ServiceEvent()
                   .withCreatedAt(Date.from(startDate.plusMinutes(9).toInstant()))
                   .withId("E4")
                   .withMessage("message"));
    events.add(new ServiceEvent()
                   .withCreatedAt(Date.from(startDate.plusMinutes(4).toInstant()))
                   .withId("E2")
                   .withMessage("message"));

    Service service = new Service()
                          .withDesiredCount(DESIRED_COUNT)
                          .withRunningCount(DESIRED_COUNT)
                          .withServiceArn("SERVICE_ARN")
                          .withEvents(events);

    List<ServiceEvent> events1 = new ArrayList<>();
    events1.add(new ServiceEvent()
                    .withCreatedAt(Date.from(startDate.plusMinutes(0).toInstant()))
                    .withId("E1")
                    .withMessage("message"));
    events1.add(new ServiceEvent()
                    .withCreatedAt(Date.from(startDate.plusMinutes(7).toInstant()))
                    .withId("E3")
                    .withMessage("message"));
    events1.add(new ServiceEvent()
                    .withCreatedAt(Date.from(startDate.plusMinutes(9).toInstant()))
                    .withId("E4")
                    .withMessage("message"));
    events1.add(new ServiceEvent()
                    .withCreatedAt(Date.from(startDate.plusMinutes(4).toInstant()))
                    .withId("E2")
                    .withMessage("message"));
    events1.add(new ServiceEvent()
                    .withCreatedAt(Date.from(startDate.plusMinutes(10).toInstant()))
                    .withId("E5")
                    .withMessage("has reached a steady state."));

    Service service1 = new Service()
                           .withDesiredCount(DESIRED_COUNT)
                           .withRunningCount(DESIRED_COUNT)
                           .withServiceArn("SERVICE_ARN")
                           .withEvents(events1);

    doReturn(new DescribeServicesResult().withServices(asList(service)))
        .doReturn(new DescribeServicesResult().withServices(asList(service1)))
        .when(awsHelperService)
        .describeServices(anyString(), any(), any(), any());

    try {
      MethodUtils.invokeMethod(ecsContainerServiceImpl, true, "waitForServiceToReachSteadyState",
          new Object[] {"use-east-1", AwsConfig.builder().build(), new ArrayList<>(), "cluster", "service", 1,
              new ExecutionLogCallback()});
      assertTrue(true);
    } catch (UncheckedTimeoutException e) {
      fail("Timeout not expected");
    }
  }
}
