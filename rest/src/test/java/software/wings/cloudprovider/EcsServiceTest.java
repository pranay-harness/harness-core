package software.wings.cloudprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.AUTO_SCALING_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.LAUNCHER_TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.EcsService;
import software.wings.service.impl.AwsHelperService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/3/17.
 */
public class EcsServiceTest extends WingsBaseTest {
  @Mock private AwsHelperService awsHelperService;
  @Mock private AmazonAutoScalingClient amazonAutoScalingClient;
  @Mock private AmazonECSClient amazonECSClient;
  @Mock private AmazonEC2Client amazonEC2Client;

  @Inject @InjectMocks private EcsService ecsService;

  private SettingAttribute connectorConfig =
      aSettingAttribute().withValue(anAwsConfig().withAccessKey(ACCESS_KEY).withSecretKey(SECRET_KEY).build()).build();

  private static final int DESIRED_CAPACITY = 2;

  @Before
  public void setUp() throws Exception {
    when(awsHelperService.getAmazonAutoScalingClient(ACCESS_KEY, SECRET_KEY)).thenReturn(amazonAutoScalingClient);
    when(awsHelperService.getAmazonEc2Client(ACCESS_KEY, SECRET_KEY)).thenReturn(amazonEC2Client);
    when(awsHelperService.getAmazonEcsClient(ACCESS_KEY, SECRET_KEY)).thenReturn(amazonECSClient);
  }

  @Test
  public void shouldProvisionNodesWithExistingAutoScalingGroup() {
    ecsService.provisionNodes(connectorConfig, AUTO_SCALING_GROUP_NAME, DESIRED_CAPACITY);
    verify(awsHelperService).getAmazonAutoScalingClient(ACCESS_KEY, SECRET_KEY);
    verify(amazonAutoScalingClient)
        .setDesiredCapacity(new SetDesiredCapacityRequest()
                                .withAutoScalingGroupName(AUTO_SCALING_GROUP_NAME)
                                .withDesiredCapacity(DESIRED_CAPACITY));
  }

  @Test
  public void shouldCreadAutoScalingGroupAndProvisionNodes() {
    DescribeAutoScalingGroupsResult autoScalingGroupsResult =
        new DescribeAutoScalingGroupsResult().withAutoScalingGroups(new AutoScalingGroup().withInstances(Arrays.asList(
            new Instance().withLifecycleState("InService"), new Instance().withLifecycleState("InService"))));
    when(amazonAutoScalingClient.describeAutoScalingGroups(
             new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(Arrays.asList(AUTO_SCALING_GROUP_NAME))))
        .thenReturn(autoScalingGroupsResult);

    when(amazonECSClient.describeClusters(new DescribeClustersRequest().withClusters(CLUSTER_NAME)))
        .thenReturn(new DescribeClustersResult().withClusters(new Cluster().withRegisteredContainerInstancesCount(2)));

    Map<String, Object> params = new HashMap<>();
    params.put("autoScalingGroupName", AUTO_SCALING_GROUP_NAME);
    params.put("clusterName", CLUSTER_NAME);
    params.put("availabilityZones", Arrays.asList("AZ1", "AZ2"));
    params.put("vpcZoneIdentifiers", "VPC_ZONE_1, VPC_ZONE_2");

    ecsService.provisionNodes(connectorConfig, DESIRED_CAPACITY, LAUNCHER_TEMPLATE_NAME, params);

    verify(awsHelperService).getAmazonEcsClient(ACCESS_KEY, SECRET_KEY);
    verify(amazonECSClient).createCluster(new CreateClusterRequest().withClusterName(CLUSTER_NAME));
    verify(amazonAutoScalingClient)
        .createAutoScalingGroup(new CreateAutoScalingGroupRequest()
                                    .withLaunchConfigurationName(LAUNCHER_TEMPLATE_NAME)
                                    .withDesiredCapacity(DESIRED_CAPACITY)
                                    .withMaxSize(2 * DESIRED_CAPACITY)
                                    .withMinSize(DESIRED_CAPACITY / 2)
                                    .withAutoScalingGroupName(AUTO_SCALING_GROUP_NAME)
                                    .withAvailabilityZones(Arrays.asList("AZ1", "AZ2"))
                                    .withVPCZoneIdentifier("VPC_ZONE_1, VPC_ZONE_2"));
    verify(amazonAutoScalingClient)
        .describeAutoScalingGroups(
            new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(Arrays.asList(AUTO_SCALING_GROUP_NAME)));
    verify(amazonECSClient).describeClusters(new DescribeClustersRequest().withClusters(CLUSTER_NAME));
  }

  @Test
  public void shouldDeployService() {
    String serviceJson =
        "{\"cluster\":\"CLUSTER_NAME\",\"desiredCount\":\"2\",\"serviceName\":\"SERVICE_NAME\",\"taskDefinition\":\"TASK_TEMPLATE\"}";
    CreateServiceRequest createServiceRequest = new CreateServiceRequest()
                                                    .withCluster(CLUSTER_NAME)
                                                    .withServiceName(SERVICE_NAME)
                                                    .withTaskDefinition("TASK_TEMPLATE")
                                                    .withDesiredCount(DESIRED_CAPACITY);
    when(amazonECSClient.createService(createServiceRequest))
        .thenReturn(new CreateServiceResult().withService(new Service().withServiceArn("SERVICE_ARN")));

    when(amazonECSClient.describeServices(
             new DescribeServicesRequest().withCluster(CLUSTER_NAME).withServices(SERVICE_NAME)))
        .thenReturn(new DescribeServicesResult().withServices(
            new Service().withRunningCount(DESIRED_CAPACITY).withDesiredCount(DESIRED_CAPACITY)));
    String serviceArn = ecsService.deployService(connectorConfig, serviceJson);

    verify(awsHelperService).getAmazonEcsClient(ACCESS_KEY, SECRET_KEY);
    verify(amazonECSClient).createService(createServiceRequest);
    assertThat(serviceArn).isEqualTo("SERVICE_ARN");
  }

  @Test
  public void shouldDeleteService() {
    ecsService.deleteService(connectorConfig, CLUSTER_NAME, SERVICE_NAME);
    verify(awsHelperService).getAmazonEcsClient(ACCESS_KEY, SECRET_KEY);
    verify(amazonECSClient)
        .deleteService(new DeleteServiceRequest().withCluster(CLUSTER_NAME).withService(SERVICE_NAME));
  }

  @Test
  public void shouldProvisionTasks() {
    ecsService.provisionTasks(connectorConfig, CLUSTER_NAME, SERVICE_NAME, DESIRED_CAPACITY);
    verify(awsHelperService).getAmazonEcsClient(ACCESS_KEY, SECRET_KEY);
    verify(amazonECSClient)
        .updateService(new UpdateServiceRequest()
                           .withCluster(CLUSTER_NAME)
                           .withService(SERVICE_NAME)
                           .withDesiredCount(DESIRED_CAPACITY));
  }
}
