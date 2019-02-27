package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.EcsSetupCommandUnit.ERROR;
import static software.wings.common.Constants.BG_GREEN;
import static software.wings.common.Constants.BG_VERSION;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsResult;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalingPoliciesRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalingPoliciesResult;
import com.amazonaws.services.applicationautoscaling.model.ScalableTarget;
import com.amazonaws.services.applicationautoscaling.model.ServiceNamespace;
import com.amazonaws.services.ecs.model.AssignPublicIp;
import com.amazonaws.services.ecs.model.AwsVpcConfiguration;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
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
import com.amazonaws.services.ecs.model.Tag;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;
import software.wings.utils.EcsConvention;

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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class EcsSetupCommandTaskHelper {
  private static final transient Logger logger = LoggerFactory.getLogger(EcsSetupCommandTaskHelper.class);
  @Inject private AwsClusterService awsClusterService;
  @Inject private AwsAppAutoScalingHelperServiceDelegate awsAppAutoScalingService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private AwsEcsHelperServiceDelegate awsEcsHelperServiceDelegate;
  @Inject private EcsContainerService ecsContainerService;
  private static final String DELIMITER = "__";
  private static final String CONTAINER_NAME_PLACEHOLDER_REGEX = "\\$\\{CONTAINER_NAME}";

  public TaskDefinition createTaskDefinition(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ExecutionLogCallback executionLogCallback, EcsSetupParams setupParams) {
    String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
    String containerName = EcsConvention.getContainerName(dockerImageName);
    String domainName = setupParams.getImageDetails().getDomainName();

    EcsContainerTask ecsContainerTask = (EcsContainerTask) setupParams.getContainerTask();
    ecsContainerTask = createEcsContainerTaskIfNull(ecsContainerTask);

    executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName(), LogLevel.INFO);
    executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);
    executionLogCallback.saveExecutionLog("Container Name: " + containerName, LogLevel.INFO);

    // create Task definition and register it with AWS
    return createTaskDefinition(ecsContainerTask, containerName, dockerImageName, setupParams, awsConfig,
        serviceVariables, safeDisplayServiceVariables, encryptedDataDetails, executionLogCallback, domainName);
  }

  @VisibleForTesting
  EcsContainerTask createEcsContainerTaskIfNull(EcsContainerTask ecsContainerTask) {
    if (ecsContainerTask == null) {
      ecsContainerTask = new EcsContainerTask();
      software.wings.beans.container.ContainerDefinition containerDefinition =
          software.wings.beans.container.ContainerDefinition.builder()
              .memory(256)
              .cpu(1)
              .portMappings(emptyList())
              .build();
      ecsContainerTask.setContainerDefinitions(newArrayList(containerDefinition));
    }

    return ecsContainerTask;
  }

  /**
   * This method will create TaskDefinition and register it with AWS.
   */
  public TaskDefinition createTaskDefinition(EcsContainerTask ecsContainerTask, String containerName,
      String dockerImageName, EcsSetupParams ecsSetupParams, AwsConfig awsConfig, Map<String, String> serviceVariables,
      Map<String, String> safeDisplayServiceVariables, List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, String domainName) {
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
    return awsClusterService.createTask(ecsSetupParams.getRegion(), aSettingAttribute().withValue(awsConfig).build(),
        encryptedDataDetails, registerTaskDefinitionRequest);
  }

  /**
   * Checks for null, "" and  "    "
   *
   * @param input
   * @return
   */
  public boolean isEmptyOrBlank(String input) {
    // empty checkd for null or 0 size, blank checks for only spaces
    if (StringUtils.isEmpty(input) || isBlank(input)) {
      return true;
    }

    return false;
  }

  /**
   * Check if this task is configured as Fargate lanuch type
   *
   * @param ecsSetupParams
   * @return
   */
  public boolean isFargateTaskLauchType(EcsSetupParams ecsSetupParams) {
    if (LaunchType.FARGATE.name().equals(ecsSetupParams.getLaunchType())) {
      return true;
    }

    return false;
  }

  /**
   * For AwsVpcMode we need to make sure NetworkConfiguration i.e. (SubnetId/s, securityGroupId/s) is provided and For
   * fargate in addition to this executionRole is also required
   *
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

  @VisibleForTesting
  Optional<Service> getLastRunningService(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      EcsSetupParams setupParams, String serviceName) {
    List<Service> sortedServiceList =
        getServicesForClusterByMatchingPrefix(awsConfig, setupParams, encryptedDataDetails, serviceName);

    if (isEmpty(sortedServiceList)) {
      return empty();
    }

    for (int i = sortedServiceList.size() - 1; i >= 0; i--) {
      Service service = sortedServiceList.get(i);
      if (service.getServiceName().equals(serviceName)) {
        continue;
      }

      if (service.getDesiredCount() > 0) {
        return of(service);
      }
    }
    return empty();
  }

  public void storeCurrentServiceNameAndCountInfo(AwsConfig awsConfig, EcsSetupParams setupParams,
      List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder, String serviceName) {
    Optional<Service> currentService = getLastRunningService(awsConfig, encryptedDataDetails, setupParams, serviceName);
    if (currentService.isPresent()) {
      commandExecutionDataBuilder.ecsServiceToBeDownsized(currentService.get().getServiceName());
      commandExecutionDataBuilder.countToBeDownsizedForOldService(currentService.get().getDesiredCount());
    }
  }

  public List<Service> getServicesForClusterByMatchingPrefix(AwsConfig awsConfig, EcsSetupParams setupParams,
      List<EncryptedDataDetail> encryptedDataDetails, String serviceName) {
    List<Service> services = awsEcsHelperServiceDelegate.listServicesForCluster(
        awsConfig, encryptedDataDetails, setupParams.getRegion(), setupParams.getClusterName());
    return services.stream()
        .filter(service -> matchWithRegex(service.getServiceName(), serviceName))
        .sorted(comparingInt(service -> getRevisionFromServiceName(service.getServiceName())))
        .collect(toList());
  }

  public boolean matchWithRegex(String serviceNameToMatch, String serviceNameForPattern) {
    String pattern = new StringBuilder(64)
                         .append("^")
                         .append(getServicePrefixByRemovingNumber(serviceNameForPattern))
                         .append("[0-9]+$")
                         .toString();
    return Pattern.compile(pattern).matcher(serviceNameToMatch).matches();
  }

  public String getServicePrefixByRemovingNumber(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return name.substring(0, index + DELIMITER.length());
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return name;
  }

  public int getRevisionFromServiceName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }

  @VisibleForTesting
  void logLoadBalancerInfo(ExecutionLogCallback executionLogCallback, EcsSetupParams setupParams) {
    if (!setupParams.isUseRoute53DNSSwap() && setupParams.isUseLoadBalancer()) {
      executionLogCallback.saveExecutionLog("Load Balancer Name: " + setupParams.getLoadBalancerName(), LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Target Group ARN: " + setupParams.getTargetGroupArn(), LogLevel.INFO);
      if (isNotBlank(setupParams.getRoleArn())) {
        executionLogCallback.saveExecutionLog("Role ARN: " + setupParams.getRoleArn(), LogLevel.INFO);
      }
    }
  }

  @VisibleForTesting
  void backupAutoScalarConfig(EcsSetupParams setupParams, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    Map<String, Integer> activeServiceCounts = awsClusterService.getActiveServiceCounts(setupParams.getRegion(),
        cloudProviderSetting, encryptedDataDetails, setupParams.getClusterName(), containerServiceName);

    if (isEmpty(activeServiceCounts)) {
      return;
    }

    List<String> resourceIds = activeServiceCounts.keySet()
                                   .stream()
                                   .map(serviceName
                                       -> new StringBuilder("service/")
                                              .append(setupParams.getClusterName())
                                              .append("/")
                                              .append(serviceName)
                                              .toString())
                                   .collect(toList());

    executionLogCallback.saveExecutionLog("Checking for Auto-Scalar config for existing services");
    DescribeScalableTargetsResult targetsResult = awsAppAutoScalingService.listScalableTargets(setupParams.getRegion(),
        (AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails,
        new DescribeScalableTargetsRequest().withServiceNamespace(ServiceNamespace.Ecs).withResourceIds(resourceIds));

    if (isEmpty(targetsResult.getScalableTargets())) {
      executionLogCallback.saveExecutionLog("No Auto-scalar config found for existing services");
      return;
    }

    Map<String, AwsAutoScalarConfig> scalarConfigMap = new HashMap<>();

    targetsResult.getScalableTargets().forEach(scalableTarget -> {
      scalarConfigMap.putIfAbsent(getAutoScalarMapKey(scalableTarget),
          AwsAutoScalarConfig.builder()
              .resourceId(scalableTarget.getResourceId())
              .scalableTargetJson(awsAppAutoScalingService.getJsonForAwsScalableTarget(scalableTarget))
              .build());

      DescribeScalingPoliciesResult policiesResult = awsAppAutoScalingService.listScalingPolicies(
          setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails,
          new DescribeScalingPoliciesRequest()
              .withResourceId(scalableTarget.getResourceId())
              .withScalableDimension(scalableTarget.getScalableDimension())
              .withServiceNamespace(scalableTarget.getServiceNamespace()));

      List<String> policyJsons = new ArrayList<>();
      AwsAutoScalarConfig config = scalarConfigMap.get(getAutoScalarMapKey(scalableTarget));
      if (isNotEmpty(policiesResult.getScalingPolicies())) {
        policiesResult.getScalingPolicies().forEach(
            scalingPolicy -> { policyJsons.add(awsAppAutoScalingService.getJsonForAwsScalablePolicy(scalingPolicy)); });
      }
      if (isNotEmpty(policyJsons)) {
        config.setScalingPolicyJson(policyJsons.toArray(new String[policyJsons.size()]));
      }
    });

    executionLogCallback.saveExecutionLog("Auto-Scalar Config backed up");
    commandExecutionDataBuilder.previousAwsAutoScalarConfigs(scalarConfigMap.values().stream().collect(toList()));
  }

  @VisibleForTesting
  String getAutoScalarMapKey(ScalableTarget scalableTarget) {
    return scalableTarget.getResourceId() + ":" + scalableTarget.getScalableDimension();
  }

  @VisibleForTesting
  List<String[]> integerMapToListOfStringArray(Map<String, Integer> integerMap) {
    return integerMap.entrySet()
        .stream()
        .map(entry -> new String[] {entry.getKey(), entry.getValue().toString()})
        .collect(toList());
  }

  public CreateServiceRequest getCreateServiceRequest(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, EcsSetupParams setupParams, TaskDefinition taskDefinition,
      String containerServiceName, ExecutionLogCallback executionLogCallback, Logger logger,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder) {
    boolean isFargateTaskType = isFargateTaskLauchType(setupParams);
    CreateServiceRequest createServiceRequest =
        new CreateServiceRequest()
            .withServiceName(containerServiceName)
            .withCluster(setupParams.getClusterName())
            .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision());

    if (setupParams.isBlueGreen()) {
      createServiceRequest.withTags(new com.amazonaws.services.ecs.model.Tag().withKey(BG_VERSION).withValue(BG_GREEN));
    }

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
    if (!setupParams.isUseRoute53DNSSwap() && setupParams.isUseLoadBalancer()) {
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
    List<ServiceRegistry> serviceRegistries = newArrayList();
    if (serviceSpecification != null && StringUtils.isNotBlank(serviceSpecification.getServiceSpecJson())) {
      // Replace $Container_NAME string if exists, with actual container name
      if (setupParams.getEcsServiceSpecification() != null
          && StringUtils.isNotBlank(setupParams.getEcsServiceSpecification().getServiceSpecJson())) {
        String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
        String containerName = EcsConvention.getContainerName(dockerImageName);
        EcsServiceSpecification specification = setupParams.getEcsServiceSpecification();
        specification.setServiceSpecJson(
            specification.getServiceSpecJson().replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName));
      }

      Service advancedServiceConfig = getAwsServiceFromJson(serviceSpecification.getServiceSpecJson(), logger);
      validateServiceRegistries(advancedServiceConfig.getServiceRegistries(), taskDefinition, executionLogCallback);

      createServiceRequest.setPlacementStrategy(advancedServiceConfig.getPlacementStrategy());
      createServiceRequest.setPlacementConstraints(advancedServiceConfig.getPlacementConstraints());
      createServiceRequest.setHealthCheckGracePeriodSeconds(advancedServiceConfig.getHealthCheckGracePeriodSeconds());
      if (isNotEmpty(advancedServiceConfig.getServiceRegistries())) {
        serviceRegistries.addAll(advancedServiceConfig.getServiceRegistries());
      }
      setDeploymentConfiguration(createServiceRequest, advancedServiceConfig);
      createServiceRequest.setTags(advancedServiceConfig.getTags());

      // This will only work for Daemon service. Reason is, these tags are only propgates for tasks
      // those are created with serviceCreation. We always create service with 0 count and
      // then upsize it in all case other than daemon (where ECS launches tasks with service creation)
      createServiceRequest.setPropagateTags(advancedServiceConfig.getPropagateTags());
    }
    setServiceRegistryForDNSSwap((AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails, setupParams,
        containerServiceName, serviceRegistries, executionLogCallback, logger, commandExecutionDataBuilder);
    createServiceRequest.setServiceRegistries(serviceRegistries);
    return createServiceRequest;
  }

  @VisibleForTesting
  void setServiceRegistryForDNSSwap(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      EcsSetupParams setupParams, String serviceName, List<ServiceRegistry> serviceRegistries,
      ExecutionLogCallback logCallback, Logger logger,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder) {
    if (!setupParams.isBlueGreen() || !setupParams.isUseRoute53DNSSwap()) {
      return;
    }
    commandExecutionDataBuilder.useRoute53Swap(true);
    commandExecutionDataBuilder.parentRecordName(setupParams.getParentRecordName());
    commandExecutionDataBuilder.parentRecordHostedZoneId(setupParams.getParentRecordHostedZoneId());

    String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
    String containerName = EcsConvention.getContainerName(dockerImageName);
    String registry1JSON =
        setupParams.getServiceDiscoveryService1JSON().replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName);
    String registry2JSON =
        setupParams.getServiceDiscoveryService2JSON().replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName);
    ServiceRegistry registry1 = getServiceRegistryFromJson(registry1JSON, logger);
    ServiceRegistry registry2 = getServiceRegistryFromJson(registry2JSON, logger);

    Optional<Service> currentRunningService =
        getLastRunningService(awsConfig, encryptedDataDetails, setupParams, serviceName);
    if (!currentRunningService.isPresent() || isEmpty(currentRunningService.get().getServiceRegistries())) {
      logCallback.saveExecutionLog("No currently running service found. OR no service registries found with it");
      logCallback.saveExecutionLog(format("Using: [%s] for new service.", registry1.getRegistryArn()));
      serviceRegistries.add(registry1);
      commandExecutionDataBuilder.newServiceDiscoveryArn(registry1.getRegistryArn());
      commandExecutionDataBuilder.oldServiceDiscoveryArn(registry2.getRegistryArn());
      return;
    }

    Set<String> registries = currentRunningService.get()
                                 .getServiceRegistries()
                                 .stream()
                                 .map(ServiceRegistry::getRegistryArn)
                                 .collect(toSet());

    ServiceRegistry oldRegistry;
    ServiceRegistry newRegistry;
    if (registries.contains(registry1.getRegistryArn())) {
      oldRegistry = registry1;
      newRegistry = registry2;
    } else if (registries.contains(registry2.getRegistryArn())) {
      oldRegistry = registry2;
      newRegistry = registry1;
    } else {
      logCallback.saveExecutionLog("Current Ecs Service not associated with any of the 2 registries.");
      newRegistry = registry1;
      oldRegistry = registry2;
    }
    logCallback.saveExecutionLog(format("Current Ess service uses: [%s]", oldRegistry.getRegistryArn()));
    commandExecutionDataBuilder.oldServiceDiscoveryArn(oldRegistry.getRegistryArn());
    logCallback.saveExecutionLog(format("Using: [%s] for new service.", newRegistry.getRegistryArn()));
    commandExecutionDataBuilder.newServiceDiscoveryArn(newRegistry.getRegistryArn());
    serviceRegistries.add(newRegistry);
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

  @VisibleForTesting
  ServiceRegistry getServiceRegistryFromJson(String json, Logger logger) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(json, ServiceRegistry.class);
    } catch (IOException e) {
      String errorMsg = "Failed to Deserialize json into AWS Service object";
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

  /**
   * Validate if ContainerName and ContianerPort if mentioned in ServiceRegistry in serviceSpec,
   * matches ones defined in TaskDefinitions
   *
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

  @VisibleForTesting
  boolean isInvalidContainerPortUsed(ServiceRegistry serviceRegistry, ContainerDefinition containerDefinition,
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

  @VisibleForTesting
  void setDeploymentConfiguration(CreateServiceRequest createServiceRequest, Service advancedServiceConfig) {
    if (advancedServiceConfig.getDeploymentConfiguration() != null
        && advancedServiceConfig.getDeploymentConfiguration().getMaximumPercent() != null
        && advancedServiceConfig.getDeploymentConfiguration().getMinimumHealthyPercent() != null) {
      createServiceRequest.setDeploymentConfiguration(advancedServiceConfig.getDeploymentConfiguration());
    }
  }

  public String createEcsService(EcsSetupParams setupParams, TaskDefinition taskDefinition,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    String containerServiceName =
        EcsConvention.getServiceName(setupParams.getTaskFamily(), taskDefinition.getRevision());

    Map<String, Integer> activeServiceCounts = awsClusterService.getActiveServiceCounts(setupParams.getRegion(),
        cloudProviderSetting, encryptedDataDetails, setupParams.getClusterName(), containerServiceName);

    commandExecutionDataBuilder.containerServiceName(containerServiceName)
        .activeServiceCounts(integerMapToListOfStringArray(activeServiceCounts));

    CreateServiceRequest createServiceRequest = getCreateServiceRequest(cloudProviderSetting, encryptedDataDetails,
        setupParams, taskDefinition, containerServiceName, executionLogCallback, logger, commandExecutionDataBuilder);

    executionLogCallback.saveExecutionLog(
        format("Creating ECS service %s in cluster %s ", containerServiceName, setupParams.getClusterName()),
        LogLevel.INFO);

    // create and register service with aws
    awsClusterService.createService(
        setupParams.getRegion(), cloudProviderSetting, encryptedDataDetails, createServiceRequest);

    return containerServiceName;
  }

  public void handleRollback(EcsSetupParams setupParams, SettingAttribute cloudProviderSetting,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    if (setupParams.isDaemonSchedulingStrategy()) {
      try {
        // For Daemon service, we cache service spec json for existing service before we did actual deployment,
        // as deployment is being rolled back, update service with same service spec to restore it to original state
        if (isNotEmpty(setupParams.getPreviousEcsServiceSnapshotJson())) {
          Service previousServiceSnapshot =
              getAwsServiceFromJson(setupParams.getPreviousEcsServiceSnapshotJson(), logger);
          UpdateServiceRequest updateServiceRequest =
              new UpdateServiceRequest()
                  .withService(previousServiceSnapshot.getServiceName())
                  .withCluster(setupParams.getClusterName())
                  .withNetworkConfiguration(previousServiceSnapshot.getNetworkConfiguration())
                  .withTaskDefinition(previousServiceSnapshot.getTaskDefinition())
                  .withDeploymentConfiguration(previousServiceSnapshot.getDeploymentConfiguration())
                  .withHealthCheckGracePeriodSeconds(previousServiceSnapshot.getHealthCheckGracePeriodSeconds());

          awsHelperService.updateService(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
              encryptedDataDetails, updateServiceRequest);
          waitForDaemonServiceToReachSteadyState(setupParams.getRegion(), cloudProviderSetting, encryptedDataDetails,
              setupParams.getClusterName(), previousServiceSnapshot.getServiceName(),
              setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
          commandExecutionDataBuilder.containerServiceName(previousServiceSnapshot.getServiceName())
              .ecsTaskDefintion(previousServiceSnapshot.getTaskDefinition())
              .ecsServiceArn(previousServiceSnapshot.getServiceArn());
        } else {
          // For Daemon service, if first launch of the service fails, we delete that service, as there is no way to set
          // 0 tasks for such service. If we dont delete it, ECS will keep trying scheduling tasks on all instances with
          // cluster
          DeleteServiceRequest deleteServiceRequest = new DeleteServiceRequest()
                                                          .withService(setupParams.getEcsServiceArn())
                                                          .withCluster(setupParams.getClusterName());
          executionLogCallback.saveExecutionLog(new StringBuilder(32)
                                                    .append("Deleting Service")
                                                    .append(setupParams.getTaskFamily())
                                                    .append(" as first launch of service failed: ")
                                                    .toString());
          awsHelperService.deleteService(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
              encryptedDataDetails, deleteServiceRequest);
        }
      } catch (Exception e) {
        String errorMsg = "Failed while handling rollback";
        logger.error(errorMsg, e);
        throw new WingsException(errorMsg, e, USER).addParam("message", errorMsg);
      }
    }
  }

  public List<ContainerInfo> waitForDaemonServiceToReachSteadyState(String region, SettingAttribute connectorConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String serviceName,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(connectorConfig, encryptedDataDetails);

    Service service = awsHelperService
                          .describeServices(region, awsConfig, encryptedDataDetails,
                              new DescribeServicesRequest().withCluster(clusterName).withServices(serviceName))
                          .getServices()
                          .get(0);

    ecsContainerService.waitForTasksToBeInRunningStateButDontThrowException(region, awsConfig, encryptedDataDetails,
        clusterName, serviceName, executionLogCallback, service.getDesiredCount());
    ecsContainerService.waitForServiceToReachSteadyState(region, awsConfig, encryptedDataDetails, clusterName,
        serviceName, serviceSteadyStateTimeout, executionLogCallback);
    return ecsContainerService.getContainerInfosAfterEcsWait(region, awsConfig, encryptedDataDetails, clusterName,
        serviceName, Collections.EMPTY_LIST, executionLogCallback, false);
  }

  public void downsizeOldOrUnhealthy(SettingAttribute settingAttribute, EcsSetupParams setupParams,
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
  public void cleanup(SettingAttribute settingAttribute, String region, String containerServiceName, String clusterName,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("\nCleaning versions with no tasks", LogLevel.INFO);
    String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceName);
    awsClusterService.getServices(region, settingAttribute, encryptedDataDetails, clusterName)
        .stream()
        .filter(service
            -> EcsConvention.getServiceNamePrefixFromServiceName(service.getServiceName()).equals(serviceNamePrefix))
        .filter(s -> !s.getServiceName().equals(containerServiceName))
        .filter(s -> s.getDesiredCount() == 0)
        .forEach(s -> {
          String oldServiceName = s.getServiceName();
          executionLogCallback.saveExecutionLog("Deleting old version: " + oldServiceName, LogLevel.INFO);
          awsClusterService.deleteService(region, settingAttribute, encryptedDataDetails, clusterName, oldServiceName);
        });
  }

  @VisibleForTesting
  boolean isServiceWithSamePrefix(String serviceName, String prefix) {
    if (prefix.length() >= serviceName.length()) {
      return false;
    }
    String temp = serviceName.substring(prefix.length());
    return temp.matches("[0-9]+");
  }

  public Optional<Service> getExistingServiceMetadataSnapshot(EcsSetupParams setupParams,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, String ecsServiceName,
      AwsHelperService awsHelperService) {
    awsHelperService.getServiceForCluster((AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails,
        setupParams.getClusterName(), setupParams.getRegion());
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

    return empty();
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

  public String getTargetGroupForDefaultAction(Listener listener, ExecutionLogCallback executionLogCallback) {
    Optional<Action> action = listener.getDefaultActions()
                                  .stream()
                                  .filter(listenerAction
                                      -> "forward".equalsIgnoreCase(listenerAction.getType())
                                          && isNotEmpty(listenerAction.getTargetGroupArn()))
                                  .findFirst();

    if (!action.isPresent()) {
      String errorMsg = new StringBuilder(128)
                            .append("No Forward Action set for Listener: ")
                            .append("ARN: ")
                            .append(listener.getListenerArn())
                            .append("Protocol: ")
                            .append(listener.getProtocol())
                            .append("Port: ")
                            .append(listener.getPort())
                            .append(", to any TargetGroup")
                            .toString();
      executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, errorMsg, USER).addParam("message", errorMsg);
    }

    return action.get().getTargetGroupArn();
  }

  public void deleteExistingServicesOtherThanBlueVersion(EcsSetupParams setupParams,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback) {
    List<Service> services = getServicesForClusterByMatchingPrefix((AwsConfig) cloudProviderSetting.getValue(),
        setupParams, encryptedDataDetails, trim(setupParams.getTaskFamily()) + DELIMITER);

    services = services.stream().filter(service -> isGreenVersion(service.getTags())).collect(toList());

    if (isNotEmpty(services)) {
      services.forEach(service -> {
        executionLogCallback.saveExecutionLog("Deleting Old Service  {Green Version}: " + service.getServiceName());
        awsHelperService.deleteService(setupParams.getRegion(), (AwsConfig) cloudProviderSetting.getValue(),
            encryptedDataDetails,
            new DeleteServiceRequest()
                .withService(service.getServiceArn())
                .withCluster(setupParams.getClusterName())
                .withForce(true));
        executionLogCallback.saveExecutionLog("Deletion successful");
      });
    }
  }

  private boolean isGreenVersion(List<Tag> tags) {
    if (isEmpty(tags)) {
      return false;
    }
    Optional<Tag> tag =
        tags.stream()
            .filter(serviceTag -> BG_VERSION.equals(serviceTag.getKey()) && BG_GREEN.equals(serviceTag.getValue()))
            .findFirst();
    return tag.isPresent();
  }
}
