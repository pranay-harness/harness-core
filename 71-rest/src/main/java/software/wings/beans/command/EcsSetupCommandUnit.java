package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.NetworkConfiguration;
import com.amazonaws.services.ecs.model.NetworkMode;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.exception.WingsException;
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
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.container.EcsContainerTask;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by brett on 11/18/17
 */
public class EcsSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsSetupCommandUnit.class);
  public static final String ERROR = "Error: ";

  @Inject @Transient private transient AwsClusterService awsClusterService;
  @Inject @Transient private transient EcsContainerService ecsContainerService;
  @Inject @Transient private transient AwsHelperService awsHelperService;

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
      String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
      String containerName = EcsConvention.getContainerName(dockerImageName);
      String domainName = setupParams.getImageDetails().getDomainName();

      EcsContainerTask ecsContainerTask = (EcsContainerTask) setupParams.getContainerTask();
      ecsContainerTask = createEcsContainerTaskIfNull(ecsContainerTask);

      executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName(), LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Container Name: " + containerName, LogLevel.INFO);

      // create Task definition and register it with AWS
      TaskDefinition taskDefinition =
          createTaskDefinition(ecsContainerTask, containerName, dockerImageName, setupParams, cloudProviderSetting,
              serviceVariables, safeDisplayServiceVariables, encryptedDataDetails, executionLogCallback, domainName);

      String containerServiceName =
          EcsConvention.getServiceName(setupParams.getTaskFamily(), taskDefinition.getRevision());

      Map<String, Integer> activeServiceCounts = awsClusterService.getActiveServiceCounts(setupParams.getRegion(),
          cloudProviderSetting, encryptedDataDetails, setupParams.getClusterName(), containerServiceName);

      commandExecutionDataBuilder.containerServiceName(containerServiceName)
          .activeServiceCounts(integerMapToListOfStringArray(activeServiceCounts));

      CreateServiceRequest createServiceRequest = getCreateServiceRequest(cloudProviderSetting, encryptedDataDetails,
          setupParams, taskDefinition, containerServiceName, executionLogCallback);

      executionLogCallback.saveExecutionLog(
          format("Creating ECS service %s in cluster %s ", containerServiceName, setupParams.getClusterName()),
          LogLevel.INFO);

      // create and register service with aws
      awsClusterService.createService(
          setupParams.getRegion(), cloudProviderSetting, encryptedDataDetails, createServiceRequest);

      try {
        // This should not halt workflow execution.
        downsizeOldOrUnhealthy(
            cloudProviderSetting, setupParams, containerServiceName, encryptedDataDetails, executionLogCallback);
      } catch (Exception e) {
        logger.warn("Cleaning up of old or unhealthy instances failed while setting up ECS service: ", e);
      }

      cleanup(cloudProviderSetting, setupParams.getRegion(), containerServiceName, setupParams.getClusterName(),
          encryptedDataDetails, executionLogCallback);

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
      logger.error(Misc.getMessage(ex), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      return CommandExecutionStatus.FAILURE;
    } finally {
      context.setCommandExecutionData(commandExecutionDataBuilder.build());
    }
  }

  private CreateServiceRequest getCreateServiceRequest(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, EcsSetupParams setupParams, TaskDefinition taskDefinition,
      String containerServiceName, ExecutionLogCallback executionLogCallback) {
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
      executionLogCallback.saveExecutionLog("Setting load balancer to service");
      setLoadBalancerToService(setupParams, cloudProviderSetting, encryptedDataDetails, taskDefinition,
          createServiceRequest, executionLogCallback);
    }

    // for Fargate, where network mode is "awsvpc", setting taskRole causes error.
    if (!isFargateTaskType) {
      createServiceRequest.withRole(setupParams.getRoleArn());
    } else {
      createServiceRequest.withLaunchType(LaunchType.FARGATE);
    }

    // Setup config related to Fargate lauch type. here we set NetworkConfig
    if (isFargateTaskType || NetworkMode.Awsvpc.name().equalsIgnoreCase(taskDefinition.getNetworkMode())) {
      AssignPublicIp assignPublicIp = AssignPublicIp.DISABLED;

      // For EC2 type deployment, assignPublicIp = Enabled is not supported even for awsvpc networkMode
      if (isFargateTaskType) {
        assignPublicIp = setupParams.isAssignPublicIps() ? AssignPublicIp.ENABLED : AssignPublicIp.DISABLED;
      }

      createServiceRequest.withNetworkConfiguration(
          new NetworkConfiguration().withAwsvpcConfiguration(new AwsVpcConfiguration()
                                                                 .withSecurityGroups(setupParams.getSecurityGroupIds())
                                                                 .withSubnets(setupParams.getSubnetIds())
                                                                 .withAssignPublicIp(assignPublicIp)));
    }
    return createServiceRequest;
  }

  private void setLoadBalancerToService(EcsSetupParams setupParams, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, TaskDefinition taskDefinition,
      CreateServiceRequest createServiceRequest, ExecutionLogCallback executionLogCallback) {
    Integer containerPort = null;
    String containerName = null;

    String targetContainerName = setupParams.getTargetContainerName();
    String targetPort = setupParams.getTargetPort();

    if (targetContainerName != null && targetPort != null) {
      containerName = targetContainerName;

      if (!StringUtils.isNumeric(targetPort.trim())) {
        StringBuilder builder =
            new StringBuilder().append("Invalid port : ").append(targetPort).append(". It should be a number");
        executionLogCallback.saveExecutionLog(ERROR + builder.toString());
        throw new WingsException(builder.toString());
      }

      containerPort = Integer.parseInt(targetPort);

    } else if (targetContainerName == null && targetPort == null) {
      TargetGroup targetGroup = awsClusterService.getTargetGroup(
          setupParams.getRegion(), cloudProviderSetting, encryptedDataDetails, setupParams.getTargetGroupArn());

      if (targetGroup == null) {
        StringBuilder builder = new StringBuilder()
                                    .append("Target group is null for the given ARN: ")
                                    .append(setupParams.getTargetGroupArn());
        executionLogCallback.saveExecutionLog(ERROR + builder.toString());
        throw new WingsException(builder.toString());
      }

      final Integer targetGroupPort = targetGroup.getPort();

      if (targetGroupPort == null) {
        StringBuilder builder = new StringBuilder()
                                    .append("Target group port is null for the given ARN: ")
                                    .append(setupParams.getTargetGroupArn());
        executionLogCallback.saveExecutionLog(ERROR + builder.toString());
        throw new WingsException(builder.toString());
      }

      List<ContainerDefinition> containerDefinitionList = taskDefinition.getContainerDefinitions();

      Multimap<ContainerDefinition, PortMapping> portMappingListWithTargetPort = HashMultimap.create();
      containerDefinitionList.forEach(containerDefinition -> {
        List<PortMapping> portMappings = containerDefinition.getPortMappings();

        if (portMappings == null) {
          return;
        }

        List<PortMapping> portMappingList =
            portMappings.stream()
                .filter(portMapping
                    -> portMapping.getContainerPort().equals(targetGroupPort)
                        || (portMapping.getHostPort() != null && portMapping.getHostPort().equals(targetGroupPort)))
                .collect(toList());
        portMappingListWithTargetPort.putAll(containerDefinition, portMappingList);
      });

      Set<ContainerDefinition> containerDefinitionSet = portMappingListWithTargetPort.keySet();
      if (isEmpty(containerDefinitionSet)) {
        StringBuilder builder = new StringBuilder()
                                    .append("No container definition has port mapping that matches the target port: ")
                                    .append(targetGroupPort)
                                    .append(" for target group: ")
                                    .append(setupParams.getTargetGroupArn());
        executionLogCallback.saveExecutionLog(ERROR + builder.toString());
        throw new WingsException(builder.toString());
      }

      int portMatchCount = containerDefinitionSet.size();
      if (portMatchCount > 1) {
        StringBuilder builder = new StringBuilder()
                                    .append("Only one port mapping should match the target port: ")
                                    .append(targetGroupPort)
                                    .append(" for target group: ")
                                    .append(setupParams.getTargetGroupArn());
        executionLogCallback.saveExecutionLog(ERROR + builder.toString());
        throw new WingsException(builder.toString());
      }

      ContainerDefinition containerDefinition = containerDefinitionSet.iterator().next();
      containerName = containerDefinition.getName();

      Collection<PortMapping> portMappings = portMappingListWithTargetPort.get(containerDefinition);

      if (isEmpty(portMappings)) {
        StringBuilder builder = new StringBuilder()
                                    .append("No container definition has port mapping that match the target port: ")
                                    .append(targetGroupPort)
                                    .append(" for target group: ")
                                    .append(setupParams.getTargetGroupArn());
        executionLogCallback.saveExecutionLog(ERROR + builder.toString());
        throw new WingsException(builder.toString());
      }

      if (portMappings.size() > 1) {
        StringBuilder builder = new StringBuilder()
                                    .append("Only one port mapping should match the target port: ")
                                    .append(targetGroupPort)
                                    .append(" for target group: ")
                                    .append(setupParams.getTargetGroupArn());
        executionLogCallback.saveExecutionLog(ERROR + builder.toString());
        throw new WingsException(builder.toString());
      }

      PortMapping portMapping = portMappings.iterator().next();

      containerPort = portMapping.getContainerPort();
    }

    List<LoadBalancer> loadBalancers;

    if (containerName != null && containerPort != null) {
      loadBalancers = asList(new LoadBalancer()
                                 .withContainerName(containerName)
                                 .withContainerPort(containerPort)
                                 .withTargetGroupArn(setupParams.getTargetGroupArn()));
      createServiceRequest.withLoadBalancers(loadBalancers);
    } else {
      StringBuilder builder =
          new StringBuilder()
              .append("Could not obtain container name and port to set to the target for target group: ")
              .append(setupParams.getTargetGroupArn());
      executionLogCallback.saveExecutionLog(ERROR + builder.toString());
      throw new WingsException(builder.toString());
    }
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
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback, String domainName) {
    TaskDefinition taskDefinition = ecsContainerTask.createTaskDefinition(
        containerName, dockerImageName, ecsSetupParams.getExecutionRoleArn(), domainName);
    // For Fargate we need to make sure NetworkConfiguration is provided
    String validationMessage = isValidateSetupParamasForECS(taskDefinition, ecsSetupParams);
    if (!isEmptyOrBlank(validationMessage)) {
      StringBuilder builder =
          new StringBuilder().append("Invalid setup params for ECS deployment: ").append(validationMessage);
      executionLogCallback.saveExecutionLog(ERROR + builder.toString());
      throw new WingsException(builder.toString());
    }

    taskDefinition.setFamily(ecsSetupParams.getTaskFamily());
    // Set service variables as environment variables
    if (isNotEmpty(serviceVariables)) {
      if (isNotEmpty(safeDisplayServiceVariables)) {
        executionLogCallback.saveExecutionLog("Setting environment variables in container definition", LogLevel.INFO);
        for (Map.Entry<String, String> entry : safeDisplayServiceVariables.entrySet()) {
          executionLogCallback.saveExecutionLog(entry.getKey() + "=" + entry.getValue(), LogLevel.INFO);
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

    // Add extra parameters for Fargate launch type
    if (isFargateTaskLauchType(ecsSetupParams)) {
      registerTaskDefinitionRequest.withExecutionRoleArn(taskDefinition.getExecutionRoleArn());
      registerTaskDefinitionRequest.withNetworkMode(NetworkMode.Awsvpc);
      registerTaskDefinitionRequest.setRequiresCompatibilities(Collections.singletonList(LaunchType.FARGATE.name()));
      registerTaskDefinitionRequest.withCpu(taskDefinition.getCpu());
      registerTaskDefinitionRequest.withMemory(taskDefinition.getMemory());
    }

    executionLogCallback.saveExecutionLog(
        format("Creating task definition %s with container image %s", ecsSetupParams.getTaskFamily(), dockerImageName),
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
    StringBuilder errorMessage = new StringBuilder(64);
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

  private void downsizeOldOrUnhealthy(SettingAttribute settingAttribute, EcsSetupParams setupParams,
      String containerServiceName, List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback) {
    Map<String, Integer> activeCounts = awsClusterService.getActiveServiceCounts(setupParams.getRegion(),
        settingAttribute, encryptedDataDetails, setupParams.getClusterName(), containerServiceName);
    String latestHealthyController = null;
    if (activeCounts.size() > 1) {
      AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
      executionLogCallback.saveExecutionLog("\nActive tasks:");
      for (Entry<String, Integer> entry : activeCounts.entrySet()) {
        String activeServiceName = entry.getKey();
        List<String> originalTaskArns = awsHelperService
                                            .listTasks(setupParams.getRegion(), awsConfig, encryptedDataDetails,
                                                new ListTasksRequest()
                                                    .withCluster(setupParams.getClusterName())
                                                    .withServiceName(activeServiceName)
                                                    .withDesiredStatus(DesiredStatus.RUNNING))
                                            .getTaskArns();
        List<ContainerInfo> containerInfos =
            ecsContainerService.getContainerInfosAfterEcsWait(setupParams.getRegion(), awsConfig, encryptedDataDetails,
                setupParams.getClusterName(), activeServiceName, originalTaskArns, executionLogCallback, false);
        boolean allContainersSuccess =
            containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
        if (allContainersSuccess) {
          latestHealthyController = activeServiceName;
        }
      }

      for (Entry<String, Integer> entry : activeCounts.entrySet()) {
        String serviceName = entry.getKey();
        if (!serviceName.equals(latestHealthyController)) {
          executionLogCallback.saveExecutionLog("");
          awsClusterService.resizeCluster(setupParams.getRegion(), settingAttribute, encryptedDataDetails,
              setupParams.getClusterName(), serviceName, entry.getValue(), 0,
              setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
        }
      }
    }
  }

  /**
   * Delete all older service with desiredCount as 0 while keeping only recent "minRevisionToKeep" no of services
   */
  private void cleanup(SettingAttribute settingAttribute, String region, String containerServiceName,
      String clusterName, List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("\nCleaning versions with no tasks", LogLevel.INFO);
    String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceName);
    awsClusterService.getServices(region, settingAttribute, encryptedDataDetails, clusterName)
        .stream()
        .filter(s -> s.getServiceName().startsWith(serviceNamePrefix))
        .filter(s -> !s.getServiceName().equals(containerServiceName))
        .filter(s -> s.getDesiredCount() == 0)
        .forEach(s -> {
          String oldServiceName = s.getServiceName();
          executionLogCallback.saveExecutionLog("Deleting old version: " + oldServiceName, LogLevel.INFO);
          awsClusterService.deleteService(region, settingAttribute, encryptedDataDetails, clusterName, oldServiceName);
        });
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
