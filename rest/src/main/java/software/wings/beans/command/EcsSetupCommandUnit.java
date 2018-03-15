package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.NetworkConfiguration;
import com.amazonaws.services.ecs.model.NetworkMode;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.container.EcsContainerTask;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by brett on 11/18/17
 */
public class EcsSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsSetupCommandUnit.class);

  @Inject @Transient private transient AwsClusterService awsClusterService;

  public EcsSetupCommandUnit() {
    super(CommandUnitType.ECS_SETUP);
    setDeploymentType(DeploymentType.ECS.name());
  }

  @Override
  protected CommandExecutionStatus executeInternal(CommandExecutionContext context,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupParams containerSetupParams, Map<String, String> serviceVariables,
      Map<String, String> safeDisplayServiceVariables, ExecutionLogCallback executionLogCallback) {
    ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder =
        ContainerSetupCommandUnitExecutionData.builder();
    try {
      EcsSetupParams setupParams = (EcsSetupParams) containerSetupParams;
      executionLogCallback.saveExecutionLog(
          "Create ECS service in cluster " + setupParams.getClusterName(), LogLevel.INFO);

      String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
      String containerName = EcsConvention.getContainerName(dockerImageName);

      EcsContainerTask ecsContainerTask = (EcsContainerTask) setupParams.getContainerTask();
      ecsContainerTask = createEcsContainerTaskIfNull(ecsContainerTask);

      // create Task definition and register it with AWS
      TaskDefinition taskDefinition =
          createTaskDefinition(ecsContainerTask, containerName, dockerImageName, setupParams, cloudProviderSetting,
              serviceVariables, safeDisplayServiceVariables, encryptedDataDetails, executionLogCallback);

      String containerServiceName =
          EcsConvention.getServiceName(setupParams.getTaskFamily(), taskDefinition.getRevision());

      commandExecutionDataBuilder.containerServiceName(containerServiceName).build();

      CreateServiceRequest createServiceRequest =
          getCreateServiceRequest(setupParams, taskDefinition, containerServiceName);

      executionLogCallback.saveExecutionLog(
          String.format("Creating ECS service %s in cluster %s", containerServiceName, setupParams.getClusterName()),
          LogLevel.INFO);

      // create and register service with aws
      awsClusterService.createService(
          setupParams.getRegion(), cloudProviderSetting, encryptedDataDetails, createServiceRequest);

      executionLogCallback.saveExecutionLog("Cleaning up old versions", LogLevel.INFO);
      cleanup(cloudProviderSetting, setupParams.getRegion(), containerServiceName, setupParams.getClusterName(),
          encryptedDataDetails, executionLogCallback);

      executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName(), LogLevel.INFO);
      executionLogCallback.saveExecutionLog("ECS Service Name: " + containerServiceName, LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);
      if (setupParams.isUseLoadBalancer()) {
        executionLogCallback.saveExecutionLog(
            "Load Balancer Name: " + setupParams.getLoadBalancerName(), LogLevel.INFO);
        executionLogCallback.saveExecutionLog("Target Group ARN: " + setupParams.getTargetGroupArn(), LogLevel.INFO);
        if (isNotBlank(setupParams.getRoleArn())) {
          executionLogCallback.saveExecutionLog("Role ARN: " + setupParams.getRoleArn(), LogLevel.INFO);
        }
      }
      return CommandExecutionStatus.SUCCESS;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      return CommandExecutionStatus.FAILURE;
    } finally {
      context.setCommandExecutionData(commandExecutionDataBuilder.build());
    }
  }

  private CreateServiceRequest getCreateServiceRequest(
      EcsSetupParams setupParams, TaskDefinition taskDefinition, String containerServiceName) {
    boolean isFargateTaskType = isFargateTaskLauchType(setupParams);
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
      createServiceRequest.withLoadBalancers(loadBalancers);

      // for Fargate, where network mode is "awsvpc", setting taskRole causes error.
      if (!isFargateTaskType) {
        createServiceRequest.withRole(setupParams.getRoleArn());
      }
    }

    // Setup config related to Fargate lauch type. here we set NetworkConfig
    if (isFargateTaskType) {
      AssignPublicIp assignPublicIp =
          setupParams.isAssignPublicIps() ? AssignPublicIp.ENABLED : AssignPublicIp.DISABLED;

      createServiceRequest.withLaunchType(LaunchType.FARGATE)
          .withNetworkConfiguration(new NetworkConfiguration().withAwsvpcConfiguration(
              new AwsVpcConfiguration()
                  .withSecurityGroups(setupParams.getSecurityGroupIds())
                  .withSubnets(setupParams.getSubnetIds())
                  .withAssignPublicIp(assignPublicIp)));
    }
    return createServiceRequest;
  }

  private EcsContainerTask createEcsContainerTaskIfNull(EcsContainerTask ecsContainerTask) {
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

    return ecsContainerTask;
  }

  /**
   *
   * This method will create TaskDefinition and register it with AWS.
   *
   * @param ecsContainerTask
   * @param containerName
   * @param dockerImageName
   * @param ecsSetupParams
   * @param settingAttribute
   * @param serviceVariables
   * @param safeDisplayServiceVariables
   * @param encryptedDataDetails
   * @param executionLogCallback
   * @return
   */
  private TaskDefinition createTaskDefinition(EcsContainerTask ecsContainerTask, String containerName,
      String dockerImageName, EcsSetupParams ecsSetupParams, SettingAttribute settingAttribute,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    TaskDefinition taskDefinition =
        ecsContainerTask.createTaskDefinition(containerName, dockerImageName, ecsSetupParams.getExecutionRoleArn());
    // For Fargate we need to make sure NetworkConfiguration is provided
    String validationMessage = isValidateSetupParamasForECS(taskDefinition, ecsSetupParams);
    if (!isEmptyOrBlank(validationMessage)) {
      throw new WingsException("Invalid setup params for ECS deployment: " + validationMessage);
    }

    taskDefinition.setFamily(ecsSetupParams.getTaskFamily());
    // Set service variables as environment variables
    if (isNotEmpty(serviceVariables)) {
      if (isNotEmpty(safeDisplayServiceVariables)) {
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

    // Add extra paramateres for Fargate launch type
    if (isFargateTaskLauchType(ecsSetupParams)) {
      registerTaskDefinitionRequest.withExecutionRoleArn(taskDefinition.getExecutionRoleArn());
      registerTaskDefinitionRequest.withNetworkMode(NetworkMode.Awsvpc);
      registerTaskDefinitionRequest.setRequiresCompatibilities(Collections.singletonList(LaunchType.FARGATE.name()));
      registerTaskDefinitionRequest.withCpu(taskDefinition.getCpu());
      registerTaskDefinitionRequest.withMemory(taskDefinition.getMemory());
    }

    executionLogCallback.saveExecutionLog(String.format("Creating task definition %s with container image %s",
                                              ecsSetupParams.getTaskFamily(), dockerImageName),
        LogLevel.INFO);
    return awsClusterService.createTask(
        ecsSetupParams.getRegion(), settingAttribute, encryptedDataDetails, registerTaskDefinitionRequest);
  }

  /**
   * For Fargate we need to make sure NetworkConfiguration i.e. (SuubnetId/s, securityGroupId/s) and executionRole is
   * not empty
   * @param taskDefinition
   * @param ecsSetupParams
   * @return
   */
  private String isValidateSetupParamasForECS(TaskDefinition taskDefinition, EcsSetupParams ecsSetupParams) {
    StringBuilder errorMessage = new StringBuilder();
    if (LaunchType.FARGATE.name().equals(ecsSetupParams.getLaunchType())) {
      if (isEmptyOrBlank(ecsSetupParams.getVpcId())) {
        errorMessage.append("VPC Id is required for fargate task");
      }

      if (ArrayUtils.isEmpty(ecsSetupParams.getSubnetIds())
          || CollectionUtils.isEmpty(Arrays.stream(ecsSetupParams.getSubnetIds())
                                         .filter(subnet -> !isEmptyOrBlank(subnet))
                                         .collect(toList()))) {
        errorMessage.append("At least 1 subnetId is required for mentioned VPC");
      }

      if (ArrayUtils.isEmpty(ecsSetupParams.getSecurityGroupIds())
          || CollectionUtils.isEmpty(Arrays.stream(ecsSetupParams.getSecurityGroupIds())
                                         .filter(securityGroup -> !isEmptyOrBlank(securityGroup))
                                         .collect(toList()))) {
        errorMessage.append("At least 1 security Group is required for mentioned VPC");
      }

      if (isEmptyOrBlank(taskDefinition.getExecutionRoleArn())) {
        errorMessage.append("Execution Role ARN is required for Fargate tasks");
      }
    }

    return errorMessage.toString();
  }

  /**
   * Checks for null, "" and  "    "
   * @param input
   * @return
   */
  private boolean isEmptyOrBlank(String input) {
    // empty checkd for null or 0 size, blank checks for only spaces
    if (StringUtils.isEmpty(input) || StringUtils.isBlank(input)) {
      return true;
    }

    return false;
  }

  /**
   * Check if this task is configured as Fargate lanuch type
   * @param ecsSetupParams
   * @return
   */
  private boolean isFargateTaskLauchType(EcsSetupParams ecsSetupParams) {
    if (LaunchType.FARGATE.name().equals(ecsSetupParams.getLaunchType())) {
      return true;
    }

    return false;
  }

  /**
   * Delete all older service with desiredCount as 0 while keeping only recent "minRevisionToKeep" no of services
   */
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
