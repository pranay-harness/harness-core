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
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterConfiguration the cluster configuration
   */
  void createCluster(SettingAttribute cloudProviderSetting, ClusterConfiguration clusterConfiguration);

  /**
   * Resize cluster.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterName          the cluster name
   * @param serviceName          the service name
   * @param desiredSize          the desired size
   * @param executionLogCallback the execution log callback
   * @return the list
   */
  List<ContainerInfo> resizeCluster(SettingAttribute cloudProviderSetting, String clusterName, String serviceName,
      Integer desiredSize, ExecutionLogCallback executionLogCallback);

  /**
   * Destroy cluster.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterName          the cluster name
   * @param serviceName          the service name
   */
  void destroyCluster(SettingAttribute cloudProviderSetting, String clusterName, String serviceName);

  /**
   * Gets services.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterName          the cluster name
   * @return the services
   */
  List<Service> getServices(SettingAttribute cloudProviderSetting, String clusterName);

  /**
   * Create service.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterConfiguration the cluster configuration
   */
  void createService(SettingAttribute cloudProviderSetting, CreateServiceRequest clusterConfiguration);

  /**
   * Create task task definition.
   *
   * @param settingAttribute              the setting attribute
   * @param registerTaskDefinitionRequest the register task definition request
   * @return the task definition
   */
  TaskDefinition createTask(
      SettingAttribute settingAttribute, RegisterTaskDefinitionRequest registerTaskDefinitionRequest);
}
