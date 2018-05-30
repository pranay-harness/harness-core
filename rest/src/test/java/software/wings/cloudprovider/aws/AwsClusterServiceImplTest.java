package software.wings.cloudprovider.aws;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.AUTO_SCALING_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.LAUNCHER_TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SERVICE_DEFINITION;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.Collections;

/**
 * Created by anubhaw on 1/3/17.
 */
public class AwsClusterServiceImplTest extends WingsBaseTest {
  @Mock private EcsContainerService ecsContainerService;
  @Inject @InjectMocks private AwsClusterService awsClusterService;

  private SettingAttribute cloudProviderSetting =
      aSettingAttribute().withValue(AwsConfig.builder().accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build()).build();
  private AwsClusterConfiguration clusterConfiguration = AwsClusterConfiguration.builder()
                                                             .name(CLUSTER_NAME)
                                                             .size(5)
                                                             .serviceDefinition(SERVICE_DEFINITION)
                                                             .launcherConfiguration(LAUNCHER_TEMPLATE_NAME)
                                                             .autoScalingGroupName(AUTO_SCALING_GROUP_NAME)
                                                             .vpcZoneIdentifiers("VPC_ZONE_1, VPC_ZONE_2")
                                                             .availabilityZones(asList("AZ1", "AZ2"))
                                                             .build();

  @Test
  public void shouldCreateCluster() {
    awsClusterService.createCluster(
        Regions.US_EAST_1.getName(), cloudProviderSetting, Collections.emptyList(), clusterConfiguration, null);

    ImmutableMap<String, Object> params =
        ImmutableMap.of("autoScalingGroupName", AUTO_SCALING_GROUP_NAME, "clusterName", CLUSTER_NAME,
            "availabilityZones", asList("AZ1", "AZ2"), "vpcZoneIdentifiers", "VPC_ZONE_1, VPC_ZONE_2");

    verify(ecsContainerService)
        .provisionNodes(Regions.US_EAST_1.getName(), cloudProviderSetting, Collections.emptyList(), 5,
            LAUNCHER_TEMPLATE_NAME, params, null);
  }

  @Test
  public void shouldResizeCluster() {
    awsClusterService.resizeCluster(Regions.US_EAST_1.getName(), cloudProviderSetting, Collections.emptyList(),
        CLUSTER_NAME, SERVICE_NAME, 0, 5, 10, new ExecutionLogCallback());
    verify(ecsContainerService)
        .provisionTasks(eq(Regions.US_EAST_1.getName()), eq(cloudProviderSetting), eq(Collections.emptyList()),
            eq(CLUSTER_NAME), eq(SERVICE_NAME), eq(0), eq(5), eq(10), any(ExecutionLogCallback.class));
  }

  @Test
  public void shouldDeleteService() {
    awsClusterService.deleteService(
        Regions.US_EAST_1.getName(), cloudProviderSetting, Collections.emptyList(), CLUSTER_NAME, SERVICE_NAME);
    verify(ecsContainerService)
        .deleteService(
            Regions.US_EAST_1.getName(), cloudProviderSetting, Collections.emptyList(), CLUSTER_NAME, SERVICE_NAME);
  }
}
