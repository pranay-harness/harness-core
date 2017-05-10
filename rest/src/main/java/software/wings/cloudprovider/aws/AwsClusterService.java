package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ClusterConfiguration;
import software.wings.cloudprovider.ContainerInfo;

import java.util.List;

/**
 * Created by anubhaw on 12/29/16.
 */
public interface AwsClusterService {
  /**
   * Create cluster.
   */
  void createCluster(String region, SettingAttribute cloudProviderSetting, ClusterConfiguration clusterConfiguration);

  /**
   * Resize cluster.
   */
  List<ContainerInfo> resizeCluster(String region, SettingAttribute cloudProviderSetting, String clusterName,
      String serviceName, Integer desiredSize, ExecutionLogCallback executionLogCallback);

  /**
   * Destroy cluster.
   */
  void destroyCluster(String region, SettingAttribute cloudProviderSetting, String clusterName, String serviceName);

  /**
   * Gets services.
   */
  List<Service> getServices(String region, SettingAttribute cloudProviderSetting, String clusterName);

  /**
   * Create service.
   */
  void createService(String region, SettingAttribute cloudProviderSetting, CreateServiceRequest clusterConfiguration);

  /**
   * Create task task definition.
   */
  TaskDefinition createTask(
      String region, SettingAttribute settingAttribute, RegisterTaskDefinitionRequest registerTaskDefinitionRequest);
}
