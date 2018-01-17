package software.wings.beans.command;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections.MapUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.EcsContainerTask;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.EcsConvention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by brett on 11/18/17
 */
public class EcsSetupCommandUnit extends ContainerSetupCommandUnit {
  @Inject @Transient protected transient AwsClusterService awsClusterService;

  public EcsSetupCommandUnit() {
    super(CommandUnitType.ECS_SETUP);
    setDeploymentType(DeploymentType.ECS.name());
  }

  @Override
  protected ContainerSetupCommandUnitExecutionData executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerSetupParams containerSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ExecutionLogCallback executionLogCallback) {
    EcsSetupParams setupParams = (EcsSetupParams) containerSetupParams;
    executionLogCallback.saveExecutionLog(
        "Create ECS service in cluster " + setupParams.getClusterName(), LogLevel.INFO);

    String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
    String containerName = EcsConvention.getContainerName(dockerImageName);

    EcsContainerTask ecsContainerTask = (EcsContainerTask) setupParams.getContainerTask();
    if (ecsContainerTask == null) {
      ecsContainerTask = new EcsContainerTask();
      software.wings.beans.container.ContainerDefinition containerDefinition =
          software.wings.beans.container.ContainerDefinition.builder()
              .memory(256)
              .cpu(1)
              .portMappings(emptyList())
              .build();
      ecsContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    TaskDefinition taskDefinition = createTaskDefinition(ecsContainerTask, containerName, dockerImageName,
        setupParams.getTaskFamily(), setupParams.getRegion(), cloudProviderSetting, serviceVariables,
        safeDisplayServiceVariables, encryptedDataDetails, executionLogCallback);

    String containerServiceName =
        EcsConvention.getServiceName(setupParams.getTaskFamily(), taskDefinition.getRevision());

    CreateServiceRequest createServiceRequest =
        new CreateServiceRequest()
            .withServiceName(containerServiceName)
            .withCluster(setupParams.getClusterName())
            .withDesiredCount(0)
            .withDeploymentConfiguration(
                new DeploymentConfiguration().withMaximumPercent(200).withMinimumHealthyPercent(100))
            .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision());

    if (setupParams.isUseLoadBalancer()) {
      List<LoadBalancer> loadBalancers =
          taskDefinition.getContainerDefinitions()
              .stream()
              .flatMap(containerDefinition
                  -> Optional.ofNullable(containerDefinition.getPortMappings())
                         .orElse(emptyList())
                         .stream()
                         .map(portMapping
                             -> new LoadBalancer()
                                    .withContainerName(containerDefinition.getName())
                                    .withContainerPort(portMapping.getContainerPort())
                                    .withTargetGroupArn(setupParams.getTargetGroupArn())))
              .collect(toList());
      createServiceRequest.withLoadBalancers(loadBalancers).withRole(setupParams.getRoleArn());
    }

    executionLogCallback.saveExecutionLog(
        String.format("Creating ECS service %s in cluster %s", containerServiceName, setupParams.getClusterName()),
        LogLevel.INFO);
    awsClusterService.createService(
        setupParams.getRegion(), cloudProviderSetting, encryptedDataDetails, createServiceRequest);

    executionLogCallback.saveExecutionLog("Cleaning up old versions", LogLevel.INFO);
    cleanup(cloudProviderSetting, setupParams.getRegion(), containerServiceName, setupParams.getClusterName(),
        encryptedDataDetails, executionLogCallback);

    executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName(), LogLevel.INFO);
    executionLogCallback.saveExecutionLog("ECS Service Name: " + containerServiceName, LogLevel.INFO);
    executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);
    if (setupParams.isUseLoadBalancer()) {
      executionLogCallback.saveExecutionLog("Load Balancer Name: " + setupParams.getLoadBalancerName(), LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Target Group ARN: " + setupParams.getTargetGroupArn(), LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Role ARN: " + setupParams.getRoleArn(), LogLevel.INFO);
    }

    return ContainerSetupCommandUnitExecutionData.builder().containerServiceName(containerServiceName).build();
  }

  private TaskDefinition createTaskDefinition(EcsContainerTask ecsContainerTask, String containerName,
      String dockerImageName, String taskFamily, String region, SettingAttribute settingAttribute,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    TaskDefinition taskDefinition = ecsContainerTask.createTaskDefinition(containerName, dockerImageName);
    taskDefinition.setFamily(taskFamily);

    // Set service variables as environment variables
    if (MapUtils.isNotEmpty(serviceVariables)) {
      if (MapUtils.isNotEmpty(safeDisplayServiceVariables)) {
        executionLogCallback.saveExecutionLog("Setting environment variables in container definition", LogLevel.INFO);
        for (String key : safeDisplayServiceVariables.keySet()) {
          executionLogCallback.saveExecutionLog(key + "=" + safeDisplayServiceVariables.get(key), LogLevel.INFO);
        }
      }
      Map<String, KeyValuePair> serviceValuePairs = serviceVariables.entrySet().stream().collect(Collectors.toMap(
          Map.Entry::getKey, entry -> new KeyValuePair().withName(entry.getKey()).withValue(entry.getValue())));
      for (ContainerDefinition containerDefinition : taskDefinition.getContainerDefinitions()) {
        Map<String, KeyValuePair> valuePairsMap = new HashMap<>();
        if (containerDefinition.getEnvironment() != null) {
          containerDefinition.getEnvironment().forEach(
              keyValuePair -> valuePairsMap.put(keyValuePair.getName(), keyValuePair));
        }
        valuePairsMap.putAll(serviceValuePairs);
        containerDefinition.setEnvironment(new ArrayList<>(valuePairsMap.values()));
      }
    }

    RegisterTaskDefinitionRequest registerTaskDefinitionRequest =
        new RegisterTaskDefinitionRequest()
            .withContainerDefinitions(taskDefinition.getContainerDefinitions())
            .withFamily(taskDefinition.getFamily())
            .withTaskRoleArn(taskDefinition.getTaskRoleArn())
            .withNetworkMode(taskDefinition.getNetworkMode())
            .withPlacementConstraints(taskDefinition.getPlacementConstraints())
            .withVolumes(taskDefinition.getVolumes());

    executionLogCallback.saveExecutionLog(
        String.format("Creating task definition %s with container image %s", taskFamily, dockerImageName),
        LogLevel.INFO);
    return awsClusterService.createTask(region, settingAttribute, encryptedDataDetails, registerTaskDefinitionRequest);
  }

  private void cleanup(SettingAttribute settingAttribute, String region, String containerServiceName,
      String clusterName, List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    int revision = getRevisionFromServiceName(containerServiceName);
    if (revision > KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision - KEEP_N_REVISIONS;
      String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceName);
      awsClusterService.getServices(region, settingAttribute, encryptedDataDetails, clusterName)
          .stream()
          .filter(s -> s.getServiceName().startsWith(serviceNamePrefix) && s.getDesiredCount() == 0)
          .collect(toList())
          .forEach(s -> {
            String oldServiceName = s.getServiceName();
            if (getRevisionFromServiceName(oldServiceName) < minRevisionToKeep) {
              executionLogCallback.saveExecutionLog("Deleting old version: " + oldServiceName, LogLevel.INFO);
              awsClusterService.deleteService(
                  region, settingAttribute, encryptedDataDetails, clusterName, oldServiceName);
            }
          });
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ECS_SETUP")
  public static class Yaml extends ContainerSetupCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.ECS_SETUP.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.ECS_SETUP.name(), deploymentType);
    }
  }
}
