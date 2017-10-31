package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/28/16.
 */
public interface EcsContainerService {
  /**
   * Provision nodes.
   */
  void provisionNodes(String region, SettingAttribute connectorConfig, List<EncryptedDataDetail> encryptedDataDetails,
      Integer clusterSize, String launchConfigName, Map<String, Object> params);

  /**
   * Deploy service string.
   */
  String deployService(String region, SettingAttribute connectorConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String serviceDefinition);

  /**
   * Delete service.
   */
  void deleteService(String region, SettingAttribute connectorConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String clusterName, String serviceName);
  /**
   * Provision tasks.
   */
  List<ContainerInfo> provisionTasks(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName, Integer desiredCount,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  /**
   * Create service.
   */
  void createService(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, CreateServiceRequest clusterConfiguration);

  /**
   * Create task task definition.
   */
  TaskDefinition createTask(String region, SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, RegisterTaskDefinitionRequest registerTaskDefinitionRequest);

  /**
   * Gets services.
   */
  List<Service> getServices(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName);
}
