package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.states.ContainerServiceSetup.DEFAULT_MAX;
import static software.wings.sm.states.ContainerServiceSetup.FIXED_INSTANCES;

import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.api.ecs.EcsListenerUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.helpers.ext.ecs.request.EcsBGListenerUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
        .withParentRecordHostedZoneId(ecsSetupStateConfig.getParentRecordHostedZoneId())
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
        .environmentType(environment.getEnvironmentType());
  }

  /**
   * This is for ECS BG
   */
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

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activityId))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  public Activity createActivity(ExecutionContext executionContext, String commandName, String stateType,
      CommandUnitType commandUnitType, ActivityService activityService) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
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
    return aDelegateTask()
        .withAppId(appId)
        .withAccountId(accountId)
        .withTaskType(taskType)
        .withEnvId(envId)
        .withWaitId(waitId)
        .withParameters(parameters)
        .withInfrastructureMappingId(infrastructureMappingId)
        .withTimeout(TimeUnit.MINUTES.toMillis(timeout))
        .build();
  }

  public ContainerServiceElement buildContainerServiceElement(ExecutionContext context,
      CommandExecutionResult executionResult, ExecutionStatus status, ImageDetails imageDetails, String maxInstanceStr,
      String fixedInstanceStr, String desiredInstanceCountStr, ResizeStrategy resizeStrategy,
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
            .useFixedInstances(FIXED_INSTANCES.equals(desiredInstanceCountStr))
            .fixedInstances(fixedInstances)
            .maxInstances(maxInstances)
            .resizeStrategy(resizeStrategy)
            .serviceSteadyStateTimeout(serviceSteadyStateTimeout)
            .clusterName(executionData.getClusterName())
            .deploymentType(DeploymentType.ECS)
            .infraMappingId(setupParams.getInfraMappingId());
    if (executionResult != null) {
      ContainerSetupCommandUnitExecutionData setupExecutionData =
          (ContainerSetupCommandUnitExecutionData) executionResult.getCommandExecutionData();
      if (setupExecutionData != null) {
        containerServiceElementBuilder.name(setupExecutionData.getContainerServiceName())
            .activeServiceCounts(setupExecutionData.getActiveServiceCounts());
        int totalActiveServiceCount = Optional.ofNullable(setupExecutionData.getActiveServiceCounts())
                                          .orElse(new ArrayList<>())
                                          .stream()
                                          .mapToInt(item -> Integer.valueOf(item[1]))
                                          .sum();
        if (totalActiveServiceCount > 0) {
          containerServiceElementBuilder.maxInstances(totalActiveServiceCount);
        }
      }
    }

    containerServiceElementBuilder.newServiceAutoScalarConfig(setupParams.getNewAwsAutoScalarConfigList());
    return containerServiceElementBuilder.build();
  }
}
