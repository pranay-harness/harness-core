package software.wings.cloudprovider;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.cloudprovider.AwsClusterConfiguration.Builder.anAwsClusterConfiguration;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.AUTO_SCALING_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.LAUNCHER_TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SERVICE_DEFINITION;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsService;

import java.util.Arrays;

/**
 * Created by anubhaw on 1/3/17.
 */
public class ClusterServiceTest extends WingsBaseTest {
  @Mock private EcsService ecsService;
  @Inject @InjectMocks private ClusterService clusterService;

  private SettingAttribute cloudProviderSetting =
      aSettingAttribute().withValue(anAwsConfig().withAccessKey(ACCESS_KEY).withSecretKey(SECRET_KEY).build()).build();
  private AwsClusterConfiguration clusterConfiguration = anAwsClusterConfiguration()
                                                             .withName(CLUSTER_NAME)
                                                             .withSize(5)
                                                             .withServiceDefinition(SERVICE_DEFINITION)
                                                             .withLauncherConfiguration(LAUNCHER_TEMPLATE_NAME)
                                                             .withAutoScalingGroupName(AUTO_SCALING_GROUP_NAME)
                                                             .withVpcZoneIdentifiers("VPC_ZONE_1, VPC_ZONE_2")
                                                             .withAvailabilityZones(Arrays.asList("AZ1", "AZ2"))
                                                             .build();

  @Test
  public void shouldCreateCluster() {
    clusterService.createCluster(cloudProviderSetting, clusterConfiguration);

    ImmutableMap<String, Object> params =
        ImmutableMap.of("autoScalingGroupName", AUTO_SCALING_GROUP_NAME, "clusterName", CLUSTER_NAME,
            "availabilityZones", Arrays.asList("AZ1", "AZ2"), "vpcZoneIdentifiers", "VPC_ZONE_1, VPC_ZONE_2");

    verify(ecsService).provisionNodes(cloudProviderSetting, 5, LAUNCHER_TEMPLATE_NAME, params);
    verify(ecsService).deployService(cloudProviderSetting, SERVICE_DEFINITION);
  }

  @Test
  public void shouldResizeCluster() {
    clusterService.resizeCluster(cloudProviderSetting, CLUSTER_NAME, SERVICE_NAME, 5, new ExecutionLogCallback());
    verify(ecsService)
        .provisionTasks(
            eq(cloudProviderSetting), eq(CLUSTER_NAME), eq(SERVICE_NAME), eq(5), any(ExecutionLogCallback.class));
  }

  @Test
  public void shouldDestroyCluster() {
    clusterService.destroyCluster(cloudProviderSetting, CLUSTER_NAME, SERVICE_NAME);
    verify(ecsService).deleteService(cloudProviderSetting, CLUSTER_NAME, SERVICE_NAME);
  }
}
