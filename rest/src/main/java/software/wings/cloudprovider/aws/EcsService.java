package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerService;

import java.util.List;

/**
 * Created by anubhaw on 12/28/16.
 */
public interface EcsService extends ContainerService {
  /**
   * Provision tasks.
   * @param connectorConfig the connector config
   * @param clusterName     the cluster name
   * @param serviceName     the service name
   * @param desiredCount    the desired count
   * @param executionLogCallback
   */
  List<String> provisionTasks(SettingAttribute connectorConfig, String clusterName, String serviceName,
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
   * Gets service desired count.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterName          the cluster name
   * @param serviceName          the service name
   * @return the service desired count
   */
  Integer getServiceDesiredCount(SettingAttribute cloudProviderSetting, String clusterName, String serviceName);
}
