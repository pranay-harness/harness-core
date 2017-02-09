package software.wings.cloudprovider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.EcsService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anubhaw on 12/29/16.
 */
@Singleton
public class ClusterServiceImpl implements ClusterService {
  @Inject private EcsService ecsService;
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

    ecsService.provisionNodes(cloudProviderSetting, awsClusterConfiguration.getSize(),
        awsClusterConfiguration.getLauncherConfiguration(), params);

    logger.info("Successfully created cluster and provisioned desired number of nodes");

    ecsService.deployService(cloudProviderSetting, awsClusterConfiguration.getServiceDefinition());
    logger.info("Service created succesfully ");
    logger.info("Successfully deployed service");
  }

  @Override
  public void resizeCluster(
      SettingAttribute cloudProviderSetting, String clusterName, String serviceName, Integer desiredSize) {
    ecsService.provisionTasks(cloudProviderSetting, clusterName, serviceName, desiredSize);
    logger.info("Successfully resized the cluster to {} size", desiredSize);
  }

  @Override
  public void destroyCluster(SettingAttribute cloudProviderSetting, String clusterName, String serviceName) {
    ecsService.deleteService(cloudProviderSetting, clusterName, serviceName);
    logger.info("Successfully deleted service {}", serviceName);
  }

  @Override
  public Integer getServiceDesiredCount(SettingAttribute cloudProviderSetting, String clusterName, String serviceName) {
    return ecsService.getServiceDesiredCount(cloudProviderSetting, clusterName, serviceName);
  }

  @Override
  public void createService(SettingAttribute cloudProviderSetting, CreateServiceRequest clusterConfiguration) {
    ecsService.createService(cloudProviderSetting, clusterConfiguration);
  }

  @Override
  public TaskDefinition createTask(
      SettingAttribute settingAttribute, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    return ecsService.createTask(settingAttribute, registerTaskDefinitionRequest);
  }
}
