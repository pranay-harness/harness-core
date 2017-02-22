package software.wings.integration;

import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.cloudprovider.aws.AwsClusterConfiguration.Builder.anAwsClusterConfiguration;

import com.google.inject.Inject;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterConfiguration;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.service.impl.AwsHelperService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    awsClusterService.createCluster(awsConnectorSetting, awsClusterConfiguration);
    // awsClusterService.destroyCluster(awsConnectorSetting, (String) params.get("CLUSTER_NAME"), (String)
    // params.get("SERVICE_NAME" + "_" + "SERVICE_VERSION"));
  }

  @Test
  public void shouldResizeCluster() {
    awsClusterService.resizeCluster(awsConnectorSetting, "demo_v1", "Account_v1", 3, null);
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

    ecsContainerService.provisionNodes(awsConnectorSetting, 5, "wins_demo_launchconfig_v1", params);
  }

  @Test
  public void shouldFetchAutoScalingGroup() {
    AwsConfig awsConfig = (AwsConfig) awsConnectorSetting.getValue();
    AmazonAutoScalingClient amazonAutoScalingClient =
        awsHelperService.getAmazonAutoScalingClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    AmazonEC2Client amazonEc2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    List<LaunchConfiguration> launchConfigurations =
        amazonAutoScalingClient
            .describeLaunchConfigurations(
                new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames("DemoTargetHosts"))
            .getLaunchConfigurations();
    LaunchConfiguration launchConfiguration = launchConfigurations.get(0);

    RunInstancesRequest runInstancesRequest =
        new RunInstancesRequest()
            .withImageId(launchConfiguration.getImageId())
            .withInstanceType(launchConfiguration.getInstanceType())
            .withMinCount(1)
            .withMaxCount(1)
            .withKeyName(launchConfiguration.getKeyName())
            .withIamInstanceProfile(
                new IamInstanceProfileSpecification().withName(launchConfiguration.getIamInstanceProfile()))
            .withSecurityGroupIds(launchConfiguration.getSecurityGroups())
            .withUserData(launchConfiguration.getUserData());

    RunInstancesResult runInstancesResult = amazonEc2Client.runInstances(runInstancesRequest);
    runInstancesResult.getReservation().getInstances().forEach(
        instance -> { System.out.println(instance.toString()); });
  }
}
