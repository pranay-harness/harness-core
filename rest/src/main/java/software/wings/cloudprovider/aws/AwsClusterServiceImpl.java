package software.wings.cloudprovider.aws;

import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.cloudprovider.ClusterConfiguration;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/29/16.
 */
@Singleton
public class AwsClusterServiceImpl implements AwsClusterService {
  private static final Logger logger = LoggerFactory.getLogger(AwsClusterServiceImpl.class);

  @Inject private EcsContainerService ecsContainerService;

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  @Override
  public void createCluster(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ClusterConfiguration clusterConfiguration,
      LogCallback logCallback) {
    AwsClusterConfiguration awsClusterConfiguration = (AwsClusterConfiguration) clusterConfiguration;

    // Provision cloud infra nodes
    Map<String, Object> params = new HashMap<>();
    params.put("availabilityZones", awsClusterConfiguration.getAvailabilityZones());
    params.put("vpcZoneIdentifiers", awsClusterConfiguration.getVpcZoneIdentifiers());
    params.put("clusterName", clusterConfiguration.getName());
    params.put("autoScalingGroupName", ((AwsClusterConfiguration) clusterConfiguration).getAutoScalingGroupName());

    ecsContainerService.provisionNodes(region, cloudProviderSetting, encryptedDataDetails,
        awsClusterConfiguration.getSize(), awsClusterConfiguration.getLauncherConfiguration(), params, logCallback);

    logger.info("Successfully created cluster and provisioned desired number of nodes");
  }

  @Override
  public List<ContainerInfo> resizeCluster(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName, int previousCount,
      int desiredCount, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    if (previousCount != desiredCount) {
      executionLogCallback.saveExecutionLog(format("Resize service [%s] in cluster [%s] from %s to %s instances",
          serviceName, clusterName, previousCount, desiredCount));
    } else {
      executionLogCallback.saveExecutionLog(
          format("Service [%s] in cluster [%s] stays at %s instances", serviceName, clusterName, previousCount));
    }
    return ecsContainerService.provisionTasks(region, cloudProviderSetting, encryptedDataDetails, clusterName,
        serviceName, previousCount, desiredCount, serviceSteadyStateTimeout, executionLogCallback);
  }

  @Override
  public void deleteService(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName) {
    ecsContainerService.deleteService(region, cloudProviderSetting, encryptedDataDetails, clusterName, serviceName);
    logger.info("Successfully deleted service {}", serviceName);
  }

  @Override
  public List<Service> getServices(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName) {
    return ecsContainerService.getServices(region, cloudProviderSetting, encryptedDataDetails, clusterName);
  }

  @Override
  public TargetGroup getTargetGroup(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String targetGroupArn) {
    return ecsContainerService.getTargetGroup(region, cloudProviderSetting, encryptedDataDetails, targetGroupArn);
  }

  @Override
  public void createService(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, CreateServiceRequest clusterConfiguration) {
    ecsContainerService.createService(region, cloudProviderSetting, encryptedDataDetails, clusterConfiguration);
  }

  @Override
  public TaskDefinition createTask(String region, SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    return ecsContainerService.createTask(
        region, settingAttribute, encryptedDataDetails, registerTaskDefinitionRequest);
  }

  @Override
  public LinkedHashMap<String, Integer> getActiveServiceCounts(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String containerServiceName) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceName);
    List<Service> activeOldServices =
        getServices(region, cloudProviderSetting, encryptedDataDetails, clusterName)
            .stream()
            .filter(service -> service.getServiceName().startsWith(serviceNamePrefix) && service.getDesiredCount() > 0)
            .sorted(comparingInt(service -> getRevisionFromServiceName(service.getServiceName())))
            .collect(toList());
    activeOldServices.forEach(service -> result.put(service.getServiceName(), service.getDesiredCount()));
    return result;
  }

  @Override
  public Map<String, String> getActiveServiceImages(String region, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String containerServiceName,
      String imagePrefix) {
    Map<String, String> result = new HashMap<>();
    String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceName);
    List<Service> activeOldServices =
        getServices(region, cloudProviderSetting, encryptedDataDetails, clusterName)
            .stream()
            .filter(service -> service.getServiceName().startsWith(serviceNamePrefix) && service.getDesiredCount() > 0)
            .sorted(comparingInt(service -> getRevisionFromServiceName(service.getServiceName())))
            .collect(toList());
    activeOldServices.forEach(service
        -> result.put(service.getServiceName(),
            ecsContainerService
                .getTaskDefinitionFromService(region, cloudProviderSetting, encryptedDataDetails, service)
                .getContainerDefinitions()
                .stream()
                .map(ContainerDefinition::getImage)
                .filter(image -> image.startsWith(imagePrefix + ":"))
                .findFirst()
                .orElse("none")));
    return result;
  }
}
