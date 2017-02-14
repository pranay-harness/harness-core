package software.wings.cloudprovider;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.kubernetes.KubernetesService;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.KubernetesConfig.Builder.aKubernetesConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.AUTO_SCALING_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.LAUNCHER_TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

/**
 * Created by brett on 2/10/17.
 */
public class KubernetesServiceTest extends WingsBaseTest {
  private static final int DESIRED_CAPACITY = 2;
  public static final String API_SERVER_URL = "apiServerUrl";
  public static final String USERNAME = "username";

  @Inject @InjectMocks private KubernetesService kubernetesService;

  private SettingAttribute connectorConfig = aSettingAttribute()
                                                 .withValue(aKubernetesConfig()
                                                                .withApiServerUrl(API_SERVER_URL)
                                                                .withUsername(USERNAME)
                                                                .withPassword(PASSWORD)
                                                                .build())
                                                 .build();

  @Before
  public void setUp() throws Exception {}

  @Test
  public void shouldProvisionNodesWithExistingAutoScalingGroup() {
    kubernetesService.provisionNodes(connectorConfig, AUTO_SCALING_GROUP_NAME, DESIRED_CAPACITY);
  }

  @Test
  public void shouldCreateAutoScalingGroupAndProvisionNodes() {
    DescribeAutoScalingGroupsResult autoScalingGroupsResult =
        new DescribeAutoScalingGroupsResult().withAutoScalingGroups(new AutoScalingGroup().withInstances(Arrays.asList(
            new Instance().withLifecycleState("InService"), new Instance().withLifecycleState("InService"))));

    Map<String, Object> params = new HashMap<>();
    params.put("autoScalingGroupName", AUTO_SCALING_GROUP_NAME);
    params.put("clusterName", CLUSTER_NAME);
    params.put("availabilityZones", Arrays.asList("AZ1", "AZ2"));
    params.put("vpcZoneIdentifiers", "VPC_ZONE_1, VPC_ZONE_2");

    kubernetesService.provisionNodes(connectorConfig, DESIRED_CAPACITY, LAUNCHER_TEMPLATE_NAME, params);
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

    String serviceArn = kubernetesService.deployService(connectorConfig, serviceJson);

    assertThat(serviceArn).isEqualTo("SERVICE_ARN");
  }

  @Test
  public void shouldDeleteService() {
    kubernetesService.deleteService(connectorConfig, CLUSTER_NAME, SERVICE_NAME);
  }

  @Test
  public void shouldProvisionTasks() {}
}
