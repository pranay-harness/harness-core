package software.wings.integration;

import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.cloudprovider.AwsClusterConfiguration.Builder.anAwsClusterConfiguration;

import com.google.inject.Inject;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.AwsClusterConfiguration;
import software.wings.cloudprovider.ClusterService;
import software.wings.cloudprovider.aws.EcsService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anubhaw on 12/29/16.
 */
public class ClusterServiceIntegrationTest extends WingsBaseTest {
  @Inject private ClusterService clusterService;
  @Inject private EcsService ecsService;

  private SettingAttribute awsConnectorSetting =
      aSettingAttribute()
          .withValue(anAwsConfig()
                         .withAccessKey("AKIAJLEKM45P4PO5QUFQ")
                         .withSecretKey("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")
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
        .withAvailabilityZones(Arrays.asList("us-east-1a", "us-east-1c", "us-east-1d", "us-east-1e")) // optional
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
    clusterService.createCluster(awsConnectorSetting, awsClusterConfiguration);
    // clusterService.destroyCluster(awsConnectorSetting, (String) params.get("CLUSTER_NAME"), (String)
    // params.get("SERVICE_NAME" + "_" + "SERVICE_VERSION"));
  }

  @Test
  public void shouldResizeCluster() {
    clusterService.resizeCluster(awsConnectorSetting, "demo_v1", "Account_v1", 3, "wins_demo_launchconfigAsg_v1");
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

    ecsService.provisionNodes(awsConnectorSetting, 5, "wins_demo_launchconfig_v1", params);
  }
}
