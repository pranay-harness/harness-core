package software.wings.integration;

import static java.util.Arrays.asList;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.cloudprovider.aws.AwsClusterConfiguration.Builder.anAwsClusterConfiguration;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import org.apache.commons.text.StrSubstitutor;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterConfiguration;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.cloudprovider.aws.EcsContainerServiceImpl;
import software.wings.service.impl.AwsHelperService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anubhaw on 12/29/16.
 */
@Ignore
public class AwsClusterServiceIntegrationTest extends WingsBaseTest {
  @Inject private AwsClusterService awsClusterService;
  @Inject private EcsContainerService ecsContainerService;
  @Inject private AwsHelperService awsHelperService;

  private SettingAttribute awsConnectorSetting =
      aSettingAttribute()
          .withValue(AwsConfig.builder()
                         .accessKey("AKIAJLEKM45P4PO5QUFQ")
                         .secretKey("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".toCharArray())
                         .build())
          .build();

  /*
  {
    "cluster": "${CLUSTER_NAME}_${SERVICE_VERSION},
    "desiredCount": "${CLUSTER_SIZE}",
    "serviceName": "${SERVICE_NAME}_${SERVICE_VERSION}",
    "taskDefinition": "${TASK_TEMPLATE}"
  }
   */
  private String serviceJson =
      "{\"cluster\":\"${CLUSTER_NAME}_${SERVICE_VERSION}\",\"desiredCount\":\"${CLUSTER_SIZE}\",\"serviceName\":\"${SERVICE_NAME}_${SERVICE_VERSION}\",\"taskDefinition\":\"${TASK_TEMPLATE}\"}";

  private String getServiceDefinition(String serviceJson, Map<String, Object> params) {
    return StrSubstitutor.replace(serviceJson, params);
  }

  private AwsClusterConfiguration getAwsClusterConfiguration(Map<String, Object> params) {
    String serviceDefinition = getServiceDefinition(serviceJson, params);
    return anAwsClusterConfiguration()
        .withName((String) params.get("CLUSTER_NAME") + "_" + params.get("SERVICE_VERSION"))
        .withSize((Integer) params.get("CLUSTER_SIZE"))
        .withServiceDefinition(serviceDefinition)
        .withLauncherConfiguration(params.get("LAUNCH_CONFIG") + "_" + params.get("SERVICE_VERSION"))
        .withAutoScalingGroupName(params.get("LAUNCH_CONFIG") + "Asg_" + params.get("SERVICE_VERSION"))
        .withVpcZoneIdentifiers("subnet-9725a6bd,subnet-42ddaf34,subnet-64d99b59,subnet-fbe268a3")
        .withAvailabilityZones(asList("us-east-1a", "us-east-1c", "us-east-1d", "us-east-1e")) // optional
        .build();
  }

  @Test
  public void shouldCreateCluster() {
    Map<String, Object> params = new HashMap<>();
    params.put("CLUSTER_NAME", "demo");
    params.put("CLUSTER_SIZE", 5);
    params.put("LAUNCH_CONFIG", "wins_demo_launchconfig");
    params.put("SERVICE_NAME", "Account");
    params.put("SERVICE_VERSION", "v1");
    params.put("TASK_TEMPLATE", "tomcat:7");

    AwsClusterConfiguration awsClusterConfiguration = getAwsClusterConfiguration(params);
    awsClusterService.createCluster(
        Regions.US_EAST_1.getName(), awsConnectorSetting, Collections.emptyList(), awsClusterConfiguration);
    // awsClusterService.destroyCluster(awsConnectorSetting, (String) params.get("CLUSTER_NAME"), (String)
    // params.get("SERVICE_NAME" + "_" + "SERVICE_VERSION"));
  }

  @Test
  public void shouldResizeCluster() {
    awsClusterService.resizeCluster(Regions.US_EAST_1.getName(), awsConnectorSetting, Collections.emptyList(),
        "demo_v1", "Account_v1", 0, 3, 10, null);
  }

  @Test
  public void shouldDeleteCluster() {}

  @Test
  public void shouldProvisionNode() {
    Map<String, Object> params1 = new HashMap<>();
    params1.put("CLUSTER_NAME", "demo");
    params1.put("CLUSTER_SIZE", 5);
    params1.put("LAUNCH_CONFIG", "wins_demo_launchconfig");
    params1.put("SERVICE_NAME", "Account");
    params1.put("SERVICE_VERSION", "v10");
    params1.put("TASK_TEMPLATE", "tomcat:7");

    AwsClusterConfiguration clusterConfiguration = getAwsClusterConfiguration(params1);

    Map<String, Object> params = new HashMap<>();
    params.put("availabilityZones", clusterConfiguration.getAvailabilityZones());
    params.put("vpcZoneIdentifiers", clusterConfiguration.getVpcZoneIdentifiers());
    params.put("clusterName", clusterConfiguration.getName());
    params.put("autoScalingGroupName", ((AwsClusterConfiguration) clusterConfiguration).getAutoScalingGroupName());

    ecsContainerService.provisionNodes(Regions.US_EAST_1.getName(), awsConnectorSetting, Collections.emptyList(), 5,
        "wins_demo_launchconfig_v1", params);
  }

  @Test
  public void shouldCreateClusterFromCFTemplate() throws InterruptedException {
    ((EcsContainerServiceImpl) ecsContainerService).createCluster();
  }
}
