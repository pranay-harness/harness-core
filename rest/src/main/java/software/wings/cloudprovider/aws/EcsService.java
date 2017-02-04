package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerService;

/**
 * Created by anubhaw on 12/28/16.
 */
public interface EcsService extends ContainerService {
  /**
   * Provision tasks.
   *  @param connectorConfig the connector config
   * @param clusterName
   * @param serviceName     the service name
   * @param desiredCount    the desired count
   */
  void provisionTasks(SettingAttribute connectorConfig, String clusterName, String serviceName, Integer desiredCount);

  void createService(SettingAttribute cloudProviderSetting, CreateServiceRequest clusterConfiguration);

  TaskDefinition createTask(
      SettingAttribute settingAttribute, RegisterTaskDefinitionRequest registerTaskDefinitionRequest);
}
