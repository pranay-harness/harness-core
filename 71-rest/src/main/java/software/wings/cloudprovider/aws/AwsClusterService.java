package software.wings.cloudprovider.aws;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.cloudprovider.ClusterConfiguration;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/29/16.
 */
public interface AwsClusterService {
  /**
   * Create cluster.
   */
  void createCluster(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ClusterConfiguration clusterConfiguration,
      LogCallback logCallback);

  /**
   * Resize cluster.
   */
  List<ContainerInfo> resizeCluster(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName, int previousCount,
      int desiredSize, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  /**
   * Delete service.
   */
  void deleteService(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName);

  /**
   * Gets services.
   */
  List<Service> getServices(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName);

  TargetGroup getTargetGroup(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String targetGroupArn);

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

  LinkedHashMap<String, Integer> getActiveServiceCounts(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String containerServiceName);

  Map<String, String> getActiveServiceImages(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String containerServiceName,
      String imagePrefix);
}
