package software.wings.cloudprovider.aws;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
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
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.Deployment;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceEvent;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
  public void testHasServiceReachedSteadyState() throws Exception {
    EcsContainerServiceImpl ecsContainerServiceImpl = (EcsContainerServiceImpl) ecsContainerService;

    Service service = new Service().withDeployments(new Deployment(), new Deployment());
    boolean ret =
        (boolean) invokeMethod(ecsContainerServiceImpl, true, "hasServiceReachedSteadyState", new Object[] {service});
    assertFalse(ret);

    service =
        new Service()
            .withDeployments(new Deployment().withUpdatedAt(new Date(10)))
            .withEvents(new ServiceEvent().withMessage("Foo has reached a steady state.").withCreatedAt(new Date(5)));
    ret = (boolean) invokeMethod(ecsContainerServiceImpl, true, "hasServiceReachedSteadyState", new Object[] {service});
    assertFalse(ret);

    service =
        new Service()
            .withDeployments(new Deployment().withUpdatedAt(new Date(10)))
            .withEvents(new ServiceEvent().withMessage("Foo has reached a steady state.").withCreatedAt(new Date(15)));
    ret = (boolean) invokeMethod(ecsContainerServiceImpl, true, "hasServiceReachedSteadyState", new Object[] {service});
    assertTrue(ret);
  }
}
