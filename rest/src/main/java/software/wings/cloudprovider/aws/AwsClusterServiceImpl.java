package software.wings.cloudprovider.aws;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ClusterConfiguration;
import software.wings.cloudprovider.ContainerInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/29/16.
 */
@Singleton
public class AwsClusterServiceImpl implements AwsClusterService {
  @Inject private EcsContainerService ecsContainerService;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private static final String DASH_STRING = "----------";

  @Override
  public void createCluster(
      String region, SettingAttribute cloudProviderSetting, ClusterConfiguration clusterConfiguration) {
    AwsClusterConfiguration awsClusterConfiguration = (AwsClusterConfiguration) clusterConfiguration;

    // Provision cloud infra nodes
    Map<String, Object> params = new HashMap<>();
    params.put("availabilityZones", awsClusterConfiguration.getAvailabilityZones());
    params.put("vpcZoneIdentifiers", awsClusterConfiguration.getVpcZoneIdentifiers());
    params.put("clusterName", clusterConfiguration.getName());
    params.put("autoScalingGroupName", ((AwsClusterConfiguration) clusterConfiguration).getAutoScalingGroupName());

    ecsContainerService.provisionNodes(region, cloudProviderSetting, awsClusterConfiguration.getSize(),
        awsClusterConfiguration.getLauncherConfiguration(), params);

    logger.info("Successfully created cluster and provisioned desired number of nodes");
  }

  @Override
  public List<ContainerInfo> resizeCluster(String region, SettingAttribute cloudProviderSetting, String clusterName,
      String serviceName, int previousCount, int desiredSize, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(String.format("Resize service [%s] in cluster [%s] from %s to %s instances",
                                              serviceName, clusterName, previousCount, desiredSize),
        LogLevel.INFO);
    List<ContainerInfo> containerInfos = ecsContainerService.provisionTasks(
        region, cloudProviderSetting, clusterName, serviceName, desiredSize, executionLogCallback);
    executionLogCallback.saveExecutionLog(
        String.format("Successfully completed resize operation.\n%s\n", DASH_STRING), LogLevel.INFO);
    return containerInfos;
  }

  @Override
  public void deleteService(
      String region, SettingAttribute cloudProviderSetting, String clusterName, String serviceName) {
    ecsContainerService.deleteService(region, cloudProviderSetting, clusterName, serviceName);
    logger.info("Successfully deleted service {}", serviceName);
  }

  @Override
  public List<Service> getServices(String region, SettingAttribute cloudProviderSetting, String clusterName) {
    return ecsContainerService.getServices(region, cloudProviderSetting, clusterName);
  }

  @Override
  public void createService(
      String region, SettingAttribute cloudProviderSetting, CreateServiceRequest clusterConfiguration) {
    ecsContainerService.createService(region, cloudProviderSetting, clusterConfiguration);
  }

  @Override
  public TaskDefinition createTask(
      String region, SettingAttribute settingAttribute, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    return ecsContainerService.createTask(region, settingAttribute, registerTaskDefinitionRequest);
  }
}
