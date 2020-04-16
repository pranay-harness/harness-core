package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.TaskType.ECS_COMMAND_TASK;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.states.ContainerServiceSetup.DEFAULT_MAX;

import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ecs.EcsBGSetupData;
import software.wings.api.ecs.EcsListenerUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.ecs.request.EcsBGListenerUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.helpers.ext.ecs.request.EcsServiceDeployRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsServiceDeployResponse;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ContainerServiceSetup.ContainerServiceSetupKeys;
import software.wings.sm.states.EcsSetupContextVariableHolder.EcsSetupContextVariableHolderBuilder;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class EcsStateHelper {
  public ContainerSetupParams buildContainerSetupParams(
      ExecutionContext context, EcsSetupStateConfig ecsSetupStateConfig) {
    Application app = ecsSetupStateConfig.getApp();
    Environment env = ecsSetupStateConfig.getEnv();
    ContainerTask containerTask = ecsSetupStateConfig.getContainerTask();

    String taskFamily = isNotBlank(ecsSetupStateConfig.getEcsServiceName())
        ? Misc.normalizeExpression(context.renderExpression(ecsSetupStateConfig.getEcsServiceName()))
        : EcsConvention.getTaskFamily(app.getName(), ecsSetupStateConfig.getServiceName(), env.getName());

    if (containerTask != null) {
      EcsContainerTask ecsContainerTask = (EcsContainerTask) containerTask;
      ecsContainerTask.getContainerDefinitions()
          .stream()
          .filter(containerDefinition -> isNotEmpty(containerDefinition.getCommands()))
          .forEach(containerDefinition
              -> containerDefinition.setCommands(
                  containerDefinition.getCommands().stream().map(context::renderExpression).collect(toList())));
      if (ecsContainerTask.getAdvancedConfig() != null) {
        ecsContainerTask.setAdvancedConfig(context.renderExpression(ecsContainerTask.getAdvancedConfig()));
      }
    }

    int serviceSteadyStateTimeout = ecsSetupStateConfig.getServiceSteadyStateTimeout();

    EcsInfrastructureMapping ecsInfrastructureMapping =
        (EcsInfrastructureMapping) ecsSetupStateConfig.getInfrastructureMapping();

    return anEcsSetupParams()
        .withAppName(app.getName())
        .withEnvName(env.getName())
        .withServiceName(ecsSetupStateConfig.getServiceName())
        .withClusterName(ecsSetupStateConfig.getClusterName())
        .withImageDetails(ecsSetupStateConfig.getImageDetails())
        .withContainerTask(containerTask)
        .withLoadBalancerName(context.renderExpression(ecsSetupStateConfig.getLoadBalancerName()))
        .withInfraMappingId(ecsSetupStateConfig.getInfrastructureMapping().getUuid())
        .withRoleArn(context.renderExpression(ecsSetupStateConfig.getRoleArn()))
        .withTargetGroupArn(context.renderExpression(ecsSetupStateConfig.getTargetGroupArn()))
        // Next 3 are to be used by Ecs BG only
        .withBlueGreen(ecsSetupStateConfig.isBlueGreen())
        .withTargetGroupArn2(context.renderExpression(ecsSetupStateConfig.getTargetGroupArn2()))
        .withProdListenerArn(context.renderExpression(ecsSetupStateConfig.getProdListenerArn()))
        .withStageListenerArn(context.renderExpression(ecsSetupStateConfig.getStageListenerArn()))
        .withStageListenerPort(context.renderExpression(ecsSetupStateConfig.getStageListenerPort()))
        .withUseDNSRoute53Swap(ecsSetupStateConfig.isUseRoute53DNSSwap())
        .withServiceDiscoveryService1JSON(
            context.renderExpression(ecsSetupStateConfig.getServiceDiscoveryService1JSON()))
        .withServiceDiscoveryService2JSON(
            context.renderExpression(ecsSetupStateConfig.getServiceDiscoveryService2JSON()))
        .withParentRecordHostedZoneId(context.renderExpression(ecsSetupStateConfig.getParentRecordHostedZoneId()))
        .withParentRecordName(context.renderExpression(ecsSetupStateConfig.getParentRecordName()))
        .withTaskFamily(taskFamily)
        .withUseLoadBalancer(ecsSetupStateConfig.isUseLoadBalancer())
        .withRegion(ecsInfrastructureMapping.getRegion())
        .withVpcId(ecsInfrastructureMapping.getVpcId())
        .withSubnetIds(getArrayFromList(ecsInfrastructureMapping.getSubnetIds()))
        .withSecurityGroupIds(getArrayFromList(ecsInfrastructureMapping.getSecurityGroupIds()))
        .withAssignPublicIps(ecsInfrastructureMapping.isAssignPublicIp())
        .withExecutionRoleArn(ecsInfrastructureMapping.getExecutionRole())
        .withLaunchType(ecsInfrastructureMapping.getLaunchType())
        .withTargetContainerName(context.renderExpression(ecsSetupStateConfig.getTargetContainerName()))
        .withTargetPort(context.renderExpression(ecsSetupStateConfig.getTargetPort()))
        .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
        .withRollback(ecsSetupStateConfig.isRollback())
        .withPreviousEcsServiceSnapshotJson(ecsSetupStateConfig.getPreviousEcsServiceSnapshotJson())
        .withEcsServiceSpecification(
            getServiceSpecWithRenderedExpression(ecsSetupStateConfig.getEcsServiceSpecification(), context))
        .withEcsServiceArn(ecsSetupStateConfig.getEcsServiceArn())
        .withIsDaemonSchedulingStrategy(ecsSetupStateConfig.isDaemonSchedulingStrategy())
        .withNewAwsAutoScalarConfigList(
            getNewAwsAutoScalarConfigListWithRenderedExpression(ecsSetupStateConfig.getAwsAutoScalarConfigs(), context))
        .build();
  }

  private EcsServiceSpecification getServiceSpecWithRenderedExpression(
      EcsServiceSpecification ecsServiceSpecification, ExecutionContext context) {
    if (ecsServiceSpecification == null || StringUtils.isBlank(ecsServiceSpecification.getServiceSpecJson())) {
      return ecsServiceSpecification;
    }

    ecsServiceSpecification.setServiceSpecJson(context.renderExpression(ecsServiceSpecification.getServiceSpecJson()));
    return ecsServiceSpecification;
  }

  private List<AwsAutoScalarConfig> getNewAwsAutoScalarConfigListWithRenderedExpression(
      List<AwsAutoScalarConfig> awsAutoScalarConfigs, ExecutionContext context) {
    if (isEmpty(awsAutoScalarConfigs)) {
      return awsAutoScalarConfigs;
    }

    awsAutoScalarConfigs.forEach(awsAutoScalarConfig -> {
      if (isNotBlank(awsAutoScalarConfig.getScalableTargetJson())) {
        awsAutoScalarConfig.setScalableTargetJson(
            context.renderExpression(awsAutoScalarConfig.getScalableTargetJson()));
      }

      if (isNotBlank(awsAutoScalarConfig.getScalingPolicyForTarget())) {
        awsAutoScalarConfig.setScalingPolicyForTarget(
            context.renderExpression(awsAutoScalarConfig.getScalingPolicyForTarget()));
        // Field " String ScalingPolicyForTarget", is only used when UI sends policyJson.
        // actual field used in all delegate tasks is "String[] ScalingPolicyJson". So we initialize this field using
        // vaoue set by UI.
        awsAutoScalarConfig.setScalingPolicyJson(new String[] {awsAutoScalarConfig.getScalingPolicyForTarget()});
      }
    });

    return awsAutoScalarConfigs;
  }

  private String[] getArrayFromList(List<String> input) {
    if (CollectionUtils.isEmpty(input)) {
      return new String[0];
    } else {
      return input.toArray(new String[0]);
    }
  }

  public ActivityBuilder getActivityBuilder(String appName, String appId, String commandName, Type type,
      ExecutionContext executionContext, String commandType, CommandUnitType commandUnitType, Environment environment) {
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    return Activity.builder()
        .applicationName(appName)
        .appId(appId)
        .commandName(commandName)
        .type(type)
        .workflowType(executionContext.getWorkflowType())
        .workflowExecutionName(executionContext.getWorkflowExecutionName())
        .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
        .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
        .commandType(commandType)
        .workflowExecutionId(executionContext.getWorkflowExecutionId())
        .workflowId(executionContext.getWorkflowId())
        .commandUnits(Collections.emptyList())
        .status(ExecutionStatus.RUNNING)
        .commandUnitType(commandUnitType)
        .environmentId(environment.getUuid())
        .environmentName(environment.getName())
        .environmentType(environment.getEnvironmentType())
        .triggeredBy(TriggeredBy.builder()
                         .email(workflowStandardParams.getCurrentUser().getEmail())
                         .name(workflowStandardParams.getCurrentUser().getName())
                         .build());
  }

  public ExecutionResponse queueDelegateTaskForEcsListenerUpdate(Application app, AwsConfig awsConfig,
      DelegateService delegateService, EcsInfrastructureMapping ecsInfrastructureMapping, String activityId,
      String envId, String commandName, EcsListenerUpdateRequestConfigData requestConfigData,
      List<EncryptedDataDetail> encryptedDataDetails) {
    EcsCommandRequest ecsCommandRequest = getEcsCommandListenerUpdateRequest(EcsCommandType.LISTENER_UPDATE_BG,
        commandName, app.getUuid(), app.getAccountId(), activityId, awsConfig, requestConfigData);

    EcsListenerUpdateStateExecutionData stateExecutionData = getListenerUpdateStateExecutionData(
        activityId, app.getAccountId(), app.getUuid(), ecsCommandRequest, commandName, requestConfigData);

    DelegateTask delegateTask =
        getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.ECS_COMMAND_TASK, activityId, envId,
            ecsInfrastructureMapping.getUuid(), new Object[] {ecsCommandRequest, encryptedDataDetails}, 10);
    delegateTask.setTags(isNotEmpty(awsConfig.getTag()) ? singletonList(awsConfig.getTag()) : null);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(activityId))
        .stateExecutionData(stateExecutionData)
        .async(true)
        .build();
  }

  public Activity createActivity(ExecutionContext executionContext, String commandName, String stateType,
      CommandUnitType commandUnitType, ActivityService activityService) {
    Application app = ((ExecutionContextImpl) executionContext).fetchRequiredApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder = getActivityBuilder(
        app.getName(), app.getUuid(), commandName, Type.Command, executionContext, stateType, commandUnitType, env);
    return activityService.save(activityBuilder.build());
  }

  public EcsCommandRequest getEcsCommandListenerUpdateRequest(EcsCommandType EcsCommandType, String commandName,
      String appId, String accountId, String activityId, AwsConfig awsConfig,
      EcsListenerUpdateRequestConfigData requestConfigData) {
    return EcsBGListenerUpdateRequest.builder()
        .commandName(commandName)
        .appId(appId)
        .accountId(accountId)
        .activityId(activityId)
        .clusterName(requestConfigData.getClusterName())
        .prodListenerArn(requestConfigData.getProdListenerArn())
        .stageListenerArn(requestConfigData.getStageListenerArn())
        .region(requestConfigData.getRegion())
        .serviceName(requestConfigData.getServiceName())
        .awsConfig(awsConfig)
        .downsizeOldService(requestConfigData.isDownsizeOldService())
        .serviceNameDownsized(requestConfigData.getServiceNameDownsized())
        .serviceCountDownsized(requestConfigData.getServiceCountDownsized())
        .rollback(requestConfigData.isRollback())
        .targetGroupForNewService(requestConfigData.getTargetGroupForNewService())
        .targetGroupForExistingService(requestConfigData.getTargetGroupForExistingService())
        .build();
  }

  public EcsListenerUpdateStateExecutionData getListenerUpdateStateExecutionData(String activityId, String appId,
      String accountId, EcsCommandRequest ecsCommandRequest, String commandName,
      EcsListenerUpdateRequestConfigData requestConfigData) {
    return EcsListenerUpdateStateExecutionData.builder()
        .activityId(activityId)
        .accountId(accountId)
        .appId(appId)
        .ecsCommandRequest(ecsCommandRequest)
        .commandName(commandName)
        .ecsListenerUpdateRequestConfigData(requestConfigData)
        .build();
  }

  public DelegateTask getDelegateTask(String accountId, String appId, TaskType taskType, String waitId, String envId,
      String infrastructureMappingId, Object[] parameters, long timeout) {
    return DelegateTask.builder()
        .appId(appId)
        .accountId(accountId)
        .envId(envId)
        .waitId(waitId)
        .data(TaskData.builder()
                  .async(true)
                  .taskType(taskType.name())
                  .parameters(parameters)
                  .timeout(TimeUnit.MINUTES.toMillis(timeout))
                  .build())
        .infrastructureMappingId(infrastructureMappingId)
        .build();
  }

  public ContainerServiceElement buildContainerServiceElement(ExecutionContext context,
      ContainerSetupCommandUnitExecutionData setupExecutionData, ExecutionStatus status, ImageDetails imageDetails,
      String maxInstanceStr, String fixedInstanceStr, String desiredInstanceCountStr, ResizeStrategy resizeStrategy,
      int serviceSteadyStateTimeout, Logger logger) {
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    EcsSetupParams setupParams = (EcsSetupParams) executionData.getContainerSetupParams();
    Integer maxVal = null;
    if (isNotBlank(maxInstanceStr)) {
      try {
        maxVal = Integer.valueOf(context.renderExpression(maxInstanceStr));
      } catch (NumberFormatException e) {
        logger.error(
            format("Invalid number format for max instances: %s", context.renderExpression(maxInstanceStr)), e);
      }
    }

    int evaluatedMaxInstances = maxVal != null ? maxVal : DEFAULT_MAX;
    int maxInstances = evaluatedMaxInstances == 0 ? DEFAULT_MAX : evaluatedMaxInstances;
    int evaluatedFixedInstances =
        isNotBlank(fixedInstanceStr) ? Integer.parseInt(context.renderExpression(fixedInstanceStr)) : maxInstances;
    int fixedInstances = evaluatedFixedInstances == 0 ? maxInstances : evaluatedFixedInstances;
    resizeStrategy = resizeStrategy == null ? RESIZE_NEW_FIRST : resizeStrategy;
    serviceSteadyStateTimeout =
        serviceSteadyStateTimeout > 0 ? serviceSteadyStateTimeout : DEFAULT_STEADY_STATE_TIMEOUT;
    ContainerServiceElementBuilder containerServiceElementBuilder =
        ContainerServiceElement.builder()
            .uuid(executionData.getServiceId())
            .image(imageDetails.getName() + ":" + imageDetails.getTag())
            .useFixedInstances(ContainerServiceSetupKeys.fixedInstances.equals(desiredInstanceCountStr))
            .fixedInstances(fixedInstances)
            .maxInstances(maxInstances)
            .resizeStrategy(resizeStrategy)
            .serviceSteadyStateTimeout(serviceSteadyStateTimeout)
            .clusterName(executionData.getClusterName())
            .deploymentType(DeploymentType.ECS)
            .infraMappingId(setupParams.getInfraMappingId());

    if (setupExecutionData != null) {
      containerServiceElementBuilder.name(setupExecutionData.getContainerServiceName())
          .activeServiceCounts(setupExecutionData.getActiveServiceCounts());

      Integer instanceCountForLatestVersion = setupExecutionData.getInstanceCountForLatestVersion();
      if (instanceCountForLatestVersion != null && instanceCountForLatestVersion.intValue() > 0) {
        containerServiceElementBuilder.maxInstances(instanceCountForLatestVersion.intValue());
      }
    }

    containerServiceElementBuilder.newServiceAutoScalarConfig(setupParams.getNewAwsAutoScalarConfigList());
    return containerServiceElementBuilder.build();
  }

  public EcsSetUpDataBag prepareBagForEcsSetUp(ExecutionContext context, int timeout,
      ArtifactCollectionUtils artifactCollectionUtils, ServiceResourceService serviceResourceService,
      InfrastructureMappingService infrastructureMappingService, SettingsService settingsService,
      SecretManager secretManager) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceId);
    if (artifact == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Artifact is null");
    }

    ImageDetails imageDetails =
        artifactCollectionUtils.fetchContainerImageDetails(artifact, context.getWorkflowExecutionId());
    Application app = workflowStandardParams.fetchRequiredApp();
    Environment env = workflowStandardParams.getEnv();

    Service service = serviceResourceService.getWithDetails(app.getUuid(), serviceId);
    ContainerTask containerTask =
        serviceResourceService.getContainerTaskByDeploymentType(app.getUuid(), serviceId, DeploymentType.ECS.name());
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());
    if (!(infrastructureMapping instanceof EcsInfrastructureMapping)) {
      throw new InvalidRequestException("Invalid infrastructure type");
    }
    EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
    SettingAttribute settingAttribute = settingsService.get(ecsInfrastructureMapping.getComputeProviderSettingId());
    if (settingAttribute == null) {
      throw new InvalidArgumentsException(Pair.of("Cloud Provider", "Missing, check service infrastructure"));
    }
    SettingValue settingValue = settingAttribute.getValue();
    if (!(settingValue instanceof AwsConfig)) {
      throw new InvalidArgumentsException(Pair.of("Cloud Provider", "Must be of type Aws Config"));
    }
    AwsConfig awsConfig = (AwsConfig) settingValue;
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());
    EcsServiceSpecification serviceSpecification =
        serviceResourceService.getEcsServiceSpecification(app.getUuid(), service.getUuid());
    if (timeout <= 0) {
      timeout = DEFAULT_STEADY_STATE_TIMEOUT;
    }

    return EcsSetUpDataBag.builder()
        .application(app)
        .service(service)
        .environment(env)
        .awsConfig(awsConfig)
        .imageDetails(imageDetails)
        .containerTask(containerTask)
        .serviceSteadyStateTimeout(timeout)
        .encryptedDataDetails(encryptedDataDetails)
        .serviceSpecification(serviceSpecification)
        .ecsInfrastructureMapping(ecsInfrastructureMapping)
        .build();
  }

  public EcsSetupContextVariableHolder renderEcsSetupContextVariables(ExecutionContext context) {
    Map<String, String> serviceVariables = context.getServiceVariables().entrySet().stream().collect(
        toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    EcsSetupContextVariableHolderBuilder builder = EcsSetupContextVariableHolder.builder();
    if (isNotEmpty(serviceVariables)) {
      serviceVariables.replaceAll((name, value) -> context.renderExpression(value));
      builder.serviceVariables(serviceVariables);
    }
    Map<String, String> safeDisplayServiceVariables = context.getSafeDisplayServiceVariables();
    if (isNotEmpty(safeDisplayServiceVariables)) {
      safeDisplayServiceVariables.replaceAll((name, value) -> context.renderExpression(value));
      builder.safeDisplayServiceVariables(safeDisplayServiceVariables);
    }
    return builder.build();
  }

  public void populateFromDelegateResponse(ContainerSetupCommandUnitExecutionData setupExecutionData,
      CommandStateExecutionData executionData, ContainerServiceElement containerServiceElement) {
    if (setupExecutionData != null) {
      // This will be non-null, when its ECS Daemon workflow
      executionData.setPreviousEcsServiceSnapshotJson(setupExecutionData.getPreviousEcsServiceSnapshotJson());
      executionData.setEcsServiceArn(setupExecutionData.getEcsServiceArn());
      executionData.setNewInstanceData(singletonList(ContainerServiceData.builder()
                                                         .name(setupExecutionData.getContainerServiceName())
                                                         .uniqueIdentifier(setupExecutionData.getEcsTaskDefintion())
                                                         .build()));
      executionData.setServiceName(setupExecutionData.getContainerServiceName());
      executionData.setLoadBalancer(containerServiceElement.getLoadBalancer());
      executionData.setPreviousAwsAutoScalarConfigs(setupExecutionData.getPreviousAwsAutoScalarConfigs());

      containerServiceElement.setPreviousAwsAutoScalarConfigs(setupExecutionData.getPreviousAwsAutoScalarConfigs());
      containerServiceElement.setNewEcsServiceName(setupExecutionData.getContainerServiceName());
      containerServiceElement.setEcsRegion(setupExecutionData.getEcsRegion());
      containerServiceElement.setTargetGroupForNewService(setupExecutionData.getTargetGroupForNewService());
      containerServiceElement.setTargetGroupForExistingService(setupExecutionData.getTargetGroupForExistingService());

      if (((EcsSetupParams) executionData.getContainerSetupParams()).isBlueGreen()) {
        EcsSetupParams ecsSetupParams = (EcsSetupParams) executionData.getContainerSetupParams();
        containerServiceElement.setEcsBGSetupData(
            EcsBGSetupData.builder()
                .prodEcsListener(ecsSetupParams.getProdListenerArn())
                .stageEcsListener(setupExecutionData.getStageEcsListener())
                .ecsBGTargetGroup1(ecsSetupParams.getTargetGroupArn())
                .ecsBGTargetGroup2(ecsSetupParams.getTargetGroupArn2())
                .ecsBlueGreen(true)
                .downsizedServiceName(setupExecutionData.getEcsServiceToBeDownsized())
                .downsizedServiceCount(setupExecutionData.getCountToBeDownsizedForOldService())
                .parentRecordName(setupExecutionData.getParentRecordName())
                .parentRecordHostedZoneId(setupExecutionData.getParentRecordHostedZoneId())
                .useRoute53Swap(setupExecutionData.isUseRoute53Swap())
                .oldServiceDiscoveryArn(setupExecutionData.getOldServiceDiscoveryArn())
                .newServiceDiscoveryArn(setupExecutionData.getNewServiceDiscoveryArn())
                .build());
      }
    }
  }

  public CommandStateExecutionData getStateExecutionData(
      EcsSetUpDataBag dataBag, String commandName, EcsSetupParams ecsSetupParams, Activity activity) {
    return aCommandStateExecutionData()
        .withServiceId(dataBag.getService().getUuid())
        .withServiceName(dataBag.getService().getName())
        .withAppId(dataBag.getApplication().getUuid())
        .withCommandName(commandName)
        .withContainerSetupParams(ecsSetupParams)
        .withClusterName(dataBag.getEcsInfrastructureMapping().getClusterName())
        .withActivityId(activity.getUuid())
        .build();
  }

  public String createAndQueueDelegateTaskForEcsServiceSetUp(
      EcsCommandRequest request, EcsSetUpDataBag dataBag, Activity activity, DelegateService delegateService) {
    DelegateTask task =
        DelegateTask.builder()
            .appId(dataBag.getApplication().getUuid())
            .accountId(dataBag.getApplication().getAccountId())
            .envId(dataBag.getEnvironment().getUuid())
            .waitId(activity.getUuid())
            .data(TaskData.builder()
                      .async(true)
                      .taskType(ECS_COMMAND_TASK.name())
                      .parameters(new Object[] {request, dataBag.getEncryptedDataDetails()})
                      .timeout(MINUTES.toMillis(dataBag.getServiceSteadyStateTimeout()))
                      .build())
            .tags(isNotEmpty(dataBag.getAwsConfig().getTag()) ? singletonList(dataBag.getAwsConfig().getTag()) : null)
            .infrastructureMappingId(dataBag.getEcsInfrastructureMapping().getUuid())
            .build();
    return delegateService.queueTask(task);
  }

  private void updateContainerElementAfterSuccessfulResize(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    ContainerServiceElement containerElement =
        context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
            .stream()
            .filter(cse -> phaseElement.getDeploymentType().equals(cse.getDeploymentType().name()))
            .filter(cse -> context.fetchInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(ContainerServiceElement.builder().build());
    containerElement.setPrevAutoscalarsAlreadyRemoved(true);
  }

  // This will be used later to add existing containerInfos as well.
  private void setNewInstanceFlag(
      List<InstanceElement> instanceElements, boolean flag, List<InstanceElement> finalInstanceElements) {
    if (isNotEmpty(instanceElements)) {
      instanceElements.forEach(instanceElement -> { instanceElement.setNewInstance(flag); });
      finalInstanceElements.addAll(instanceElements);
    }
  }

  public ExecutionResponse handleDelegateResponseForEcsDeploy(ExecutionContext context,
      Map<String, ResponseData> response, boolean rollback, ActivityService activityService,
      ServiceTemplateService serviceTemplateService, ContainerDeploymentManagerHelper containerDeploymentHelper) {
    String activityId = response.keySet().iterator().next();
    EcsCommandExecutionResponse executionResponse = (EcsCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        CommandExecutionStatus.SUCCESS == executionResponse.getCommandExecutionStatus() ? SUCCESS : FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();

    EcsServiceDeployResponse deployResponse = (EcsServiceDeployResponse) executionResponse.getEcsCommandResponse();
    InstanceElementListParam listParam = InstanceElementListParam.builder().build();

    if (deployResponse != null) {
      if (isNotEmpty(deployResponse.getContainerInfos())) {
        List<InstanceStatusSummary> instanceStatusSummaries =
            containerDeploymentHelper.getInstanceStatusSummaries(context, deployResponse.getContainerInfos());
        executionData.setNewInstanceStatusSummaries(instanceStatusSummaries);

        List<InstanceElement> finalInstanceElements = new ArrayList<>();
        List<InstanceElement> instanceElements =
            instanceStatusSummaries.stream().map(InstanceStatusSummary::getInstanceElement).collect(toList());
        setNewInstanceFlag(instanceElements, true, finalInstanceElements);

        listParam = InstanceElementListParam.builder().instanceElements(finalInstanceElements).build();
      }

      executionData.setOldInstanceData(deployResponse.getOldInstanceData());
      executionData.setNewInstanceData(deployResponse.getNewInstanceData());
      executionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    }

    if (!rollback && SUCCESS == executionStatus) {
      updateContainerElementAfterSuccessfulResize(context);
    }

    return ExecutionResponse.builder()
        .stateExecutionData(executionData)
        .executionStatus(executionStatus)
        .contextElement(listParam)
        .notifyElement(listParam)
        .build();
  }

  public String createAndQueueDelegateTaskForEcsServiceDeploy(EcsDeployDataBag deployDataBag,
      EcsServiceDeployRequest request, Activity activity, DelegateService delegateService) {
    DelegateTask task = DelegateTask.builder()
                            .accountId(deployDataBag.getApp().getAccountId())
                            .appId(deployDataBag.getApp().getUuid())
                            .waitId(activity.getUuid())
                            .tags(isNotEmpty(deployDataBag.getAwsConfig().getTag())
                                    ? singletonList(deployDataBag.getAwsConfig().getTag())
                                    : null)
                            .data(TaskData.builder()
                                      .async(true)
                                      .taskType(TaskType.ECS_COMMAND_TASK.name())
                                      .parameters(new Object[] {request, deployDataBag.getEncryptedDataDetails()})
                                      .timeout(MINUTES.toMillis(getTimeout(deployDataBag)))
                                      .build())
                            .envId(deployDataBag.getEnv().getUuid())
                            .infrastructureMappingId(deployDataBag.getEcsInfrastructureMapping().getUuid())
                            .build();
    return delegateService.queueTask(task);
  }

  long getTimeout(EcsDeployDataBag deployDataBag) {
    return deployDataBag.getContainerElement().getServiceSteadyStateTimeout() + 30l;
  }

  public EcsDeployDataBag prepareBagForEcsDeploy(ExecutionContext context,
      ServiceResourceService serviceResourceService, InfrastructureMappingService infrastructureMappingService,
      SettingsService settingsService, SecretManager secretManager) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.fetchRequiredApp();
    Environment env = workflowStandardParams.getEnv();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    Service svc = serviceResourceService.getWithDetails(app.getUuid(), serviceId);
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());
    if (!(infrastructureMapping instanceof EcsInfrastructureMapping)) {
      throw new InvalidRequestException(
          format("Invalid infrmapping type: [%s]", infrastructureMapping.getClass().getName()));
    }
    EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
    SettingAttribute settingAttribute = settingsService.get(ecsInfrastructureMapping.getComputeProviderSettingId());
    SettingValue settingValue = settingAttribute.getValue();
    if (!(settingValue instanceof AwsConfig)) {
      throw new InvalidRequestException(format("Invalid setting value type: [%s]", settingValue.getClass().getName()));
    }
    AwsConfig awsConfig = (AwsConfig) settingValue;
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());
    String region = ecsInfrastructureMapping.getRegion();
    ContainerServiceElement containerElement =
        context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
            .stream()
            .filter(cse -> phaseElement.getDeploymentType().equals(cse.getDeploymentType().name()))
            .filter(cse -> context.fetchInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(ContainerServiceElement.builder().build());
    ContainerRollbackRequestElement rollbackElement = context.getContextElement(
        ContextElementType.PARAM, ContainerRollbackRequestElement.CONTAINER_ROLLBACK_REQUEST_PARAM);

    return EcsDeployDataBag.builder()
        .app(app)
        .env(env)
        .service(svc)
        .region(region)
        .awsConfig(awsConfig)
        .rollbackElement(rollbackElement)
        .containerElement(containerElement)
        .encryptedDataDetails(encryptionDetails)
        .ecsInfrastructureMapping(ecsInfrastructureMapping)
        .build();
  }
}