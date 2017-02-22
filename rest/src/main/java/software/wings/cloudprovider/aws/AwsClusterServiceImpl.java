package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ClusterConfiguration;

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

  @Override
  public void createCluster(SettingAttribute cloudProviderSetting, ClusterConfiguration clusterConfiguration) {
    AwsClusterConfiguration awsClusterConfiguration = (AwsClusterConfiguration) clusterConfiguration;

    // Provision cloud infra nodes
    Map<String, Object> params = new HashMap<>();
    params.put("availabilityZones", awsClusterConfiguration.getAvailabilityZones());
    params.put("vpcZoneIdentifiers", awsClusterConfiguration.getVpcZoneIdentifiers());
    params.put("clusterName", clusterConfiguration.getName());
    params.put("autoScalingGroupName", ((AwsClusterConfiguration) clusterConfiguration).getAutoScalingGroupName());

    ecsContainerService.provisionNodes(cloudProviderSetting, awsClusterConfiguration.getSize(),
        awsClusterConfiguration.getLauncherConfiguration(), params);

    logger.info("Successfully created cluster and provisioned desired number of nodes");

    ecsContainerService.deployService(cloudProviderSetting, awsClusterConfiguration.getServiceDefinition());
    logger.info("Service created successfully");
    logger.info("Successfully deployed service");
  }

  @Override
  public List<String> resizeCluster(SettingAttribute cloudProviderSetting, String clusterName, String serviceName,
      Integer desiredSize, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Begin resizing the cluster to " + desiredSize + " size", LogLevel.INFO);
    List<String> containerInstanceArns = ecsContainerService.provisionTasks(
        cloudProviderSetting, clusterName, serviceName, desiredSize, executionLogCallback);
    executionLogCallback.saveExecutionLog(
        "Successfully resized the cluster to " + desiredSize + " size", LogLevel.INFO);
    return containerInstanceArns;
  }

  @Override
  public void destroyCluster(SettingAttribute cloudProviderSetting, String clusterName, String serviceName) {
    ecsContainerService.deleteService(cloudProviderSetting, clusterName, serviceName);
    logger.info("Successfully deleted service {}", serviceName);
  }

  @Override
  public Integer getServiceDesiredCount(SettingAttribute cloudProviderSetting, String clusterName, String serviceName) {
    return ecsContainerService.getServiceDesiredCount(cloudProviderSetting, clusterName, serviceName);
  }

  @Override
  public void createService(SettingAttribute cloudProviderSetting, CreateServiceRequest clusterConfiguration) {
    ecsContainerService.createService(cloudProviderSetting, clusterConfiguration);
  }

  @Override
  public TaskDefinition createTask(
      SettingAttribute settingAttribute, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    return ecsContainerService.createTask(settingAttribute, registerTaskDefinitionRequest);
  }
}
