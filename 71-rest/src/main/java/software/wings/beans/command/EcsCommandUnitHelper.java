package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.command.EcsSetupCommandUnit.ERROR;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.NetworkConfiguration;
import com.amazonaws.services.ecs.model.NetworkMode;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.SchedulingStrategy;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceRegistry;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class EcsCommandUnitHelper {
  public Optional<Service> getExistingServiceMetadataSnapshot(EcsSetupParams setupParams,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, String ecsServiceName,
      AwsHelperService awsHelperService) {
    ListServicesRequest listServicesRequest = new ListServicesRequest().withCluster(setupParams.getClusterName());
    ListServicesResult listServicesResult = awsHelperService.listServices(setupParams.getRegion(),
        (AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails, listServicesRequest);

    List<Service> services = new ArrayList<>();

    if (isNotEmpty(listServicesResult.getServiceArns())) {
      do {
        services.addAll(awsHelperService
                            .describeServices(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
                                encryptedDataDetails,
                                new DescribeServicesRequest()
                                    .withCluster(setupParams.getClusterName())
                                    .withServices(listServicesResult.getServiceArns()))
                            .getServices());

        listServicesRequest.setNextToken(listServicesResult.getNextToken());
      } while (listServicesResult.getNextToken() != null && listServicesResult.getServiceArns().size() == 10);

      Optional<Service> serviceOptional =
          services.stream().filter(service -> service.getServiceName().equals(ecsServiceName)).findFirst();
      if (serviceOptional.isPresent()) {
        return serviceOptional;
      }
    }

    return Optional.empty();
  }

  public CreateServiceRequest getCreateServiceRequest(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, EcsSetupParams setupParams, TaskDefinition taskDefinition,
      String containerServiceName, AwsClusterService awsClusterService, ExecutionLogCallback executionLogCallback,
      Logger logger) {
    boolean isFargateTaskType = isFargateTaskLauchType(setupParams);
    CreateServiceRequest createServiceRequest =
        new CreateServiceRequest()
            .withServiceName(containerServiceName)
            .withCluster(setupParams.getClusterName())
            .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision());

    // For DAEMON scheduling Strategy, no desired count is required.
    // Its automatically calculated by ECS based on number of instances in cluster
    if (!setupParams.isDaemonSchedulingStrategy()) {
      createServiceRequest.setDesiredCount(0);
      createServiceRequest.withDeploymentConfiguration(
          new DeploymentConfiguration().withMaximumPercent(200).withMinimumHealthyPercent(100));
      createServiceRequest.setSchedulingStrategy(SchedulingStrategy.REPLICA.name());
    } else {
      createServiceRequest.setSchedulingStrategy(SchedulingStrategy.DAEMON.name());
      createServiceRequest.withDeploymentConfiguration(
          new DeploymentConfiguration().withMaximumPercent(100).withMinimumHealthyPercent(50));
    }

    // Set load balancer config
    if (setupParams.isUseLoadBalancer()) {
      executionLogCallback.saveExecutionLog("Setting load balancer to service");
      setLoadBalancerToService(setupParams, cloudProviderSetting, encryptedDataDetails, taskDefinition,
          createServiceRequest, awsClusterService, executionLogCallback);
    }

    // for Fargate, where network mode is "awsvpc", setting taskRole causes error.
    if (!isFargateTaskType) {
      createServiceRequest.withRole(setupParams.getRoleArn());
    } else {
      createServiceRequest.withLaunchType(LaunchType.FARGATE);
    }

    // For Awsvpc Network mode (Fargate / ECS Ec2 deployment with awsvpc mode), we need to setup
    // NetworkConfig, as it will be used by aws to create ENI
    if (isFargateTaskType || NetworkMode.Awsvpc.name().equalsIgnoreCase(taskDefinition.getNetworkMode())) {
      AssignPublicIp assignPublicIp = AssignPublicIp.DISABLED;

      if (isFargateTaskType) {
        assignPublicIp = setupParams.isAssignPublicIps() ? AssignPublicIp.ENABLED : AssignPublicIp.DISABLED;
      }

      createServiceRequest.withNetworkConfiguration(
          new NetworkConfiguration().withAwsvpcConfiguration(new AwsVpcConfiguration()
                                                                 .withSecurityGroups(setupParams.getSecurityGroupIds())
                                                                 .withSubnets(setupParams.getSubnetIds())
                                                                 .withAssignPublicIp(assignPublicIp)));
    }

    // Handle Advanced Scenario (This is ECS Service json spec provided by user)
    EcsServiceSpecification serviceSpecification = setupParams.getEcsServiceSpecification();
    if (serviceSpecification != null && StringUtils.isNotBlank(serviceSpecification.getServiceSpecJson())) {
      Service advancedServiceConfig = getAwsServiceFromJson(serviceSpecification.getServiceSpecJson(), logger);
      validateServiceRegistries(advancedServiceConfig.getServiceRegistries(), taskDefinition, executionLogCallback);

      createServiceRequest.setPlacementStrategy(advancedServiceConfig.getPlacementStrategy());
      createServiceRequest.setPlacementConstraints(advancedServiceConfig.getPlacementConstraints());
      createServiceRequest.setHealthCheckGracePeriodSeconds(advancedServiceConfig.getHealthCheckGracePeriodSeconds());
      createServiceRequest.setServiceRegistries(advancedServiceConfig.getServiceRegistries());
      setDeploymentConfiguration(createServiceRequest, advancedServiceConfig);
    }

    return createServiceRequest;
  }

  /**
   * Validate if ContainerName and ContianerPort if mentioned in ServiceRegistry in serviceSpec,
   * matches ones defined in TaskDefinitions
   * @param serviceRegistries
   * @param taskDefinition
   * @param executionLogCallback
   * @return
   */
  public void validateServiceRegistries(List<ServiceRegistry> serviceRegistries, TaskDefinition taskDefinition,
      ExecutionLogCallback executionLogCallback) {
    // Validate containerNames in Service Registries match the ones defined in TaskDefinition
    Map<String, ContainerDefinition> nameToContainerDefinitionMap = new HashMap<>();
    taskDefinition.getContainerDefinitions().forEach(
        containerDefinition -> nameToContainerDefinitionMap.put(containerDefinition.getName(), containerDefinition));

    serviceRegistries.forEach(serviceRegistry -> {
      if (StringUtils.isNotBlank(serviceRegistry.getContainerName())) {
        ContainerDefinition containerDefinition = nameToContainerDefinitionMap.get(serviceRegistry.getContainerName());

        // if Container Name is not null, Validate ContainerName is mentioned in ServiceRegistry
        if (containerDefinition == null) {
          String errorMsg = new StringBuilder("Invalid Container name :")
                                .append(serviceRegistry.getContainerName())
                                .append(", mentioned in Service Registry")
                                .toString();
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR);
          throw new WingsException(errorMsg, USER).addParam("message", errorMsg);
        }

        // If containerName is mentioned, ContainerPort mapped to that name in TaskjDefinition must be used
        if (serviceRegistry.getContainerPort() == null
            || isInvalidContainerPortUsed(serviceRegistry, containerDefinition, executionLogCallback)) {
          String errorMsg = new StringBuilder("Invalid Container Port: ")
                                .append(serviceRegistry.getContainerPort())
                                .append(", mentioned in Service Registry for Container Name: ")
                                .append(serviceRegistry.getContainerName())
                                .toString();
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR);
          throw new WingsException(errorMsg, USER).addParam("message", errorMsg);
        }
      }
    });
  }

  private boolean isInvalidContainerPortUsed(ServiceRegistry serviceRegistry, ContainerDefinition containerDefinition,
      ExecutionLogCallback executionLogCallback) {
    List<PortMapping> portMappings = containerDefinition.getPortMappings();
    Optional<PortMapping> optionalPortMapping =
        portMappings.stream()
            .filter(portMapping -> serviceRegistry.getContainerPort().compareTo(portMapping.getContainerPort()) == 0)
            .findFirst();

    if (!optionalPortMapping.isPresent()) {
      return true;
    }

    return false;
  }

  private void setDeploymentConfiguration(CreateServiceRequest createServiceRequest, Service advancedServiceConfig) {
    if (advancedServiceConfig.getDeploymentConfiguration() != null
        && advancedServiceConfig.getDeploymentConfiguration().getMaximumPercent() != null
        && advancedServiceConfig.getDeploymentConfiguration().getMinimumHealthyPercent() != null) {
      createServiceRequest.setDeploymentConfiguration(advancedServiceConfig.getDeploymentConfiguration());
    }
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
  public TaskDefinition createTaskDefinition(EcsContainerTask ecsContainerTask, String containerName,
      String dockerImageName, EcsSetupParams ecsSetupParams, SettingAttribute settingAttribute,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback, String domainName,
      AwsClusterService awsClusterService) {
    TaskDefinition taskDefinition = ecsContainerTask.createTaskDefinition(
        containerName, dockerImageName, ecsSetupParams.getExecutionRoleArn(), domainName);

    // For Awsvpc mode we need to make sure NetworkConfiguration is provided
    String validationMessage = isValidateSetupParamasForECS(taskDefinition, ecsSetupParams);
    if (!isEmptyOrBlank(validationMessage)) {
      StringBuilder builder =
          new StringBuilder().append("Invalid setup params for ECS deployment: ").append(validationMessage);
      executionLogCallback.saveExecutionLog(builder.toString(), LogLevel.ERROR);
      throw new WingsException(builder.toString(), USER).addParam("message", builder.toString());
    }

    taskDefinition.setFamily(ecsSetupParams.getTaskFamily());

    // Set service variables as environment variables
    if (isNotEmpty(serviceVariables)) {
      if (isNotEmpty(safeDisplayServiceVariables)) {
        executionLogCallback.saveExecutionLog("Setting environment variables in container definition", LogLevel.INFO);
        for (Entry<String, String> entry : safeDisplayServiceVariables.entrySet()) {
          executionLogCallback.saveExecutionLog(entry.getKey() + "=" + entry.getValue(), LogLevel.INFO);
        }
      }
      Map<String, KeyValuePair> serviceValuePairs = serviceVariables.entrySet().stream().collect(Collectors.toMap(
          Entry::getKey, entry -> new KeyValuePair().withName(entry.getKey()).withValue(entry.getValue())));
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

    if (isNotEmpty(taskDefinition.getExecutionRoleArn())) {
      registerTaskDefinitionRequest.withExecutionRoleArn(taskDefinition.getExecutionRoleArn());
    }

    // Add extra parameters for Fargate launch type
    if (isFargateTaskLauchType(ecsSetupParams)) {
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
   * For AwsVpcMode we need to make sure NetworkConfiguration i.e. (SubnetId/s, securityGroupId/s) is provided and For
   * fargate in addition to this executionRole is also required
   * @param taskDefinition
   * @param ecsSetupParams
   * @return
   */
  public String isValidateSetupParamasForECS(TaskDefinition taskDefinition, EcsSetupParams ecsSetupParams) {
    StringBuilder errorMessage = new StringBuilder(128);
    if (LaunchType.FARGATE.name().equals(ecsSetupParams.getLaunchType())
        || NetworkMode.Awsvpc.name().equals(taskDefinition.getNetworkMode())) {
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
    }

    if (LaunchType.FARGATE.name().equals(ecsSetupParams.getLaunchType())) {
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
  public boolean isEmptyOrBlank(String input) {
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
  public boolean isFargateTaskLauchType(EcsSetupParams ecsSetupParams) {
    if (LaunchType.FARGATE.name().equals(ecsSetupParams.getLaunchType())) {
      return true;
    }

    return false;
  }

  public void downsizeOldOrUnhealthy(SettingAttribute settingAttribute, EcsSetupParams setupParams,
      String containerServiceName, List<EncryptedDataDetail> encryptedDataDetails, AwsClusterService awsClusterService,
      AwsHelperService awsHelperService, EcsContainerService ecsContainerService,
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
  public void cleanup(SettingAttribute settingAttribute, String region, String containerServiceName, String clusterName,
      List<EncryptedDataDetail> encryptedDataDetails, AwsClusterService awsClusterService,
      ExecutionLogCallback executionLogCallback) {
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

  public void setLoadBalancerToService(EcsSetupParams setupParams, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, TaskDefinition taskDefinition,
      CreateServiceRequest createServiceRequest, AwsClusterService awsClusterService,
      ExecutionLogCallback executionLogCallback) {
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

  public String getJsonForAwsServiceConfig(Service service, Logger logger) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(service);
    } catch (JsonProcessingException e) {
      String errorMsg = "Failed to Serialize AWS Service object into json";
      logger.error(errorMsg);
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }

  public Service getAwsServiceFromJson(String json, Logger logger) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(json, Service.class);
    } catch (IOException e) {
      String errorMsg = "Failed to Deserialize json into AWS Service object";
      logger.error(errorMsg);
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }
}
