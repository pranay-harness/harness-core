package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RunTaskRequest;
import com.amazonaws.services.ecs.model.RunTaskResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceEvent;
import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import io.harness.container.ContainerInfo;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.UpdateServiceCountRequestData;

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
      Integer clusterSize, String launchConfigName, Map<String, Object> params, LogCallback logCallback);

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
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName, int previousCount,
      int desiredCount, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  void waitForTasksToBeInRunningStateButDontThrowException(UpdateServiceCountRequestData requestData);

  void waitForServiceToReachSteadyState(int serviceSteadyStateTimeout, UpdateServiceCountRequestData requestData);

  List<ContainerInfo> getContainerInfosAfterEcsWait(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      List<String> originalTaskArns, ExecutionLogCallback executionLogCallback);

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

  RunTaskResult triggerRunTask(String region, SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, RunTaskRequest runTaskRequest);

  /**
   * Gets services.
   */
  List<Service> getServices(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName);

  TargetGroup getTargetGroup(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String targetGroupArn);

  List<ContainerInfo> generateContainerInfos(List<Task> tasks, String clusterName, String region,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback, AwsConfig awsConfig,
      List<String> taskArns, List<String> originalTaskArns);

  TaskDefinition getTaskDefinitionFromService(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, Service service);

  List<ContainerInfo> waitForDaemonServiceToReachSteadyState(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  void updateServiceCount(UpdateServiceCountRequestData updateServiceCountRequestData);

  List<ServiceEvent> getServiceEvents(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName);

  List<ServiceEvent> getEventsFromService(Service service);
}
