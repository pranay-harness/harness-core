package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/28/16.
 */
public interface EcsContainerService {
  /**
   * Provision nodes.
   *
   * @param connectorConfig      the connector config
   * @param autoScalingGroupName the auto scaling group name
   * @param clusterSize          the cluster size
   */
  void provisionNodes(SettingAttribute connectorConfig, String autoScalingGroupName, Integer clusterSize);

  /**
   * Provision nodes.
   *
   * @param connectorConfig  the connector config
   * @param clusterSize      the cluster size
   * @param launchConfigName the launch config name
   * @param params           the params
   */
  void provisionNodes(
      SettingAttribute connectorConfig, Integer clusterSize, String launchConfigName, Map<String, Object> params);

  /**
   * Deploy service string.
   *
   * @param connectorConfig   the connector config
   * @param serviceDefinition the service definition
   * @return the string
   */
  String deployService(SettingAttribute connectorConfig, String serviceDefinition);

  /**
   * Delete service.
   *  @param connectorConfig the connector config
   * @param clusterName
   * @param serviceName     the service name
   */
  void deleteService(SettingAttribute connectorConfig, String clusterName, String serviceName);
  /**
   * Provision tasks.
   *
   * @param connectorConfig      the connector config
   * @param clusterName          the cluster name
   * @param serviceName          the service name
   * @param desiredCount         the desired count
   * @param executionLogCallback the execution log callback
   * @return the list
   */
  List<ContainerInfo> provisionTasks(SettingAttribute connectorConfig, String clusterName, String serviceName,
      Integer desiredCount, ExecutionLogCallback executionLogCallback);

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

  /**
   * Gets services.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterName          the cluster name
   * @return the services
   */
  List<Service> getServices(SettingAttribute cloudProviderSetting, String clusterName);
}
