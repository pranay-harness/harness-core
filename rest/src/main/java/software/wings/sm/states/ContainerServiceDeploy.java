package software.wings.sm.states;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.ContainerServiceData.ContainerServiceDataBuilder.aContainerServiceData;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.ResizeStrategy.DOWNSIZE_OLD_FIRST;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.inject.Inject;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.Log;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionData;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by brett on 4/7/17
 */
public abstract class ContainerServiceDeploy extends State {
  static final int KEEP_N_REVISIONS = 3;

  private static final Logger logger = LoggerFactory.getLogger(ContainerServiceDeploy.class);

  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient DelegateService delegateService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient ActivityService activityService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient ServiceTemplateService serviceTemplateService;
  @Inject @Transient protected transient LogService logService;

  ContainerServiceDeploy(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    logger.info("Executing container service deploy");
    ContextData contextData = buildContextData(context);

    String activityId =
        activityService
            .save(anActivity()
                      .withAppId(contextData.appId)
                      .withApplicationName(contextData.app.getName())
                      .withEnvironmentId(contextData.env.getUuid())
                      .withEnvironmentName(contextData.env.getName())
                      .withEnvironmentType(contextData.env.getEnvironmentType())
                      .withServiceId(contextData.service.getUuid())
                      .withServiceName(contextData.service.getName())
                      .withCommandName(contextData.command.getName())
                      .withType(Type.Command)
                      .withWorkflowExecutionId(context.getWorkflowExecutionId())
                      .withWorkflowType(context.getWorkflowType())
                      .withWorkflowExecutionName(context.getWorkflowExecutionName())
                      .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                      .withStateExecutionInstanceName(context.getStateExecutionInstanceName())
                      .withCommandUnits(serviceResourceService.getFlattenCommandUnitList(contextData.app.getUuid(),
                          contextData.serviceId, contextData.env.getUuid(), contextData.command.getName()))
                      .withCommandType(contextData.command.getCommandUnitType().name())
                      .withServiceVariables(context.getServiceVariables())
                      .build())
            .getUuid();

    CommandStateExecutionData commandStateExecutionData = buildStateExecutionData(contextData, activityId);

    if (contextData.containerServiceElement.getResizeStrategy() == RESIZE_NEW_FIRST) {
      return addNewInstances(contextData, commandStateExecutionData);
    } else {
      return downsizeOldInstances(contextData, commandStateExecutionData);
    }
  }

  private CommandStateExecutionData buildStateExecutionData(ContextData contextData, String activityId) {
    CommandStateExecutionData.Builder executionDataBuilder =
        aCommandStateExecutionData()
            .withServiceId(contextData.service.getUuid())
            .withServiceName(contextData.service.getName())
            .withAppId(contextData.app.getUuid())
            .withCommandName(getCommandName())
            .withClusterName(contextData.containerServiceElement.getClusterName())
            .withActivityId(activityId);

    if (!isRollback()) {
      logger.info("Executing resize");
      List<ContainerServiceData> newInstanceDataList = new ArrayList<>();
      ContainerServiceData newInstanceData = getNewInstanceData(contextData, activityId);
      newInstanceDataList.add(newInstanceData);
      executionDataBuilder.withNewInstanceData(newInstanceDataList);
      executionDataBuilder.withOldInstanceData(getOldInstanceData(contextData, newInstanceData));
    } else {
      logger.info("Executing rollback");
      executionDataBuilder.withNewInstanceData(contextData.rollbackElement.getNewInstanceData());
      executionDataBuilder.withOldInstanceData(contextData.rollbackElement.getOldInstanceData());
    }

    return executionDataBuilder.build();
  }

  private ContainerServiceData getNewInstanceData(ContextData contextData, String activityId) {
    Optional<Integer> previousDesiredCount =
        getServiceDesiredCount(contextData.settingAttribute, contextData.region, contextData.containerServiceElement);
    if (!previousDesiredCount.isPresent()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          "Service setup not done, serviceName: " + contextData.containerServiceElement.getName());
    }
    int previousCount = previousDesiredCount.get();
    int desiredCount = getInstanceUnitType() == InstanceUnitType.PERCENTAGE
        ? Math.max((Math.min(getInstanceCount(), 100) * contextData.containerServiceElement.getMaxInstances()) / 100, 1)
        : Math.min(getInstanceCount(), contextData.containerServiceElement.getMaxInstances());

    if (desiredCount <= previousCount) {
      String msg = "Desired instance count is less than or equal to the current instance count: {current: "
          + previousCount + ", desired: " + desiredCount + "}";
      logger.warn(msg);
      logService.save(aLog()
                          .withAppId(contextData.appId)
                          .withActivityId(activityId)
                          .withCommandUnitName(contextData.commandUnitName)
                          .withLogLine(msg)
                          .withLogLevel(Log.LogLevel.WARN)
                          .build());
    }

    if (desiredCount > contextData.containerServiceElement.getMaxInstances()) {
      String msg = "Desired instance count is greater than the maximum instance count: {maximum: "
          + contextData.containerServiceElement.getMaxInstances() + ", desired: " + desiredCount + "}";
      logger.error(msg);
      logService.save(aLog()
                          .withAppId(contextData.appId)
                          .withActivityId(activityId)
                          .withCommandUnitName(contextData.commandUnitName)
                          .withLogLine(msg)
                          .withLogLevel(Log.LogLevel.WARN)
                          .build());

      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", msg);
    }

    return aContainerServiceData()
        .withName(contextData.containerServiceElement.getName())
        .withPreviousCount(previousCount)
        .withDesiredCount(desiredCount)
        .build();
  }

  private List<ContainerServiceData> getOldInstanceData(ContextData contextData, ContainerServiceData newServiceData) {
    List<ContainerServiceData> desiredCounts = new ArrayList<>();
    LinkedHashMap<String, Integer> oldServiceCounts = getOldServiceCounts(contextData);
    int downsizeCount = Math.max(newServiceData.getDesiredCount() - newServiceData.getPreviousCount(), 0);
    for (String serviceName : oldServiceCounts.keySet()) {
      int previousCount = oldServiceCounts.get(serviceName);
      int desiredCount = Math.max(previousCount - downsizeCount, 0);
      if (previousCount != desiredCount) {
        desiredCounts.add(aContainerServiceData()
                              .withName(serviceName)
                              .withPreviousCount(previousCount)
                              .withDesiredCount(desiredCount)
                              .build());
      }
      downsizeCount -= previousCount - desiredCount;
    }
    return desiredCounts;
  }

  private ExecutionResponse addNewInstances(
      ContextData contextData, CommandStateExecutionData commandStateExecutionData) {
    List<ContainerServiceData> desiredCounts = commandStateExecutionData.getNewInstanceData();
    if (desiredCounts == null || desiredCounts.isEmpty()) {
      // No instances to add; continue execution. This happens during a rollback where the new version was the first
      // one.
      return handleNewInstancesAdded(contextData, commandStateExecutionData);
    }
    commandStateExecutionData.setDownsize(false);
    logger.info("Adding instances for {} services", desiredCounts.size());
    desiredCounts.forEach(dc
        -> logger.info("Changing desired count for service {} from {} to {}", dc.getName(), dc.getPreviousCount(),
            dc.getDesiredCount()));

    CommandExecutionContext commandExecutionContext =
        buildCommandExecutionContext(contextData, desiredCounts, commandStateExecutionData.getActivityId());

    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(contextData.app.getAccountId())
                                      .withAppId(contextData.appId)
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(commandStateExecutionData.getActivityId())
                                      .withParameters(new Object[] {contextData.command, commandExecutionContext})
                                      .withEnvId(contextData.env.getUuid())
                                      .withInfrastructureMappingId(contextData.infrastructureMappingId)
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(commandStateExecutionData.getActivityId()))
        .withStateExecutionData(commandStateExecutionData)
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private ExecutionResponse downsizeOldInstances(
      ContextData contextData, CommandStateExecutionData commandStateExecutionData) {
    List<ContainerServiceData> desiredCounts = commandStateExecutionData.getOldInstanceData();
    if (desiredCounts == null || desiredCounts.isEmpty()) {
      // No instances to downsize; continue execution.
      return handleOldInstancesDownsized(contextData, commandStateExecutionData);
    }
    commandStateExecutionData.setDownsize(true);
    logger.info("Downsizing {} services", desiredCounts.size());
    desiredCounts.forEach(dc
        -> logger.info("Changing desired count for service {} from {} to {}", dc.getName(), dc.getPreviousCount(),
            dc.getDesiredCount()));

    CommandExecutionContext commandExecutionContext =
        buildCommandExecutionContext(contextData, desiredCounts, commandStateExecutionData.getActivityId());

    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(contextData.app.getAccountId())
                                      .withAppId(contextData.appId)
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(commandStateExecutionData.getActivityId())
                                      .withParameters(new Object[] {contextData.command, commandExecutionContext})
                                      .withEnvId(contextData.env.getUuid())
                                      .withInfrastructureMappingId(contextData.infrastructureMappingId)
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(commandStateExecutionData.getActivityId()))
        .withStateExecutionData(commandStateExecutionData)
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    logger.info("Received async response");
    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();
    CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();

    if (commandExecutionResult == null || commandExecutionResult.getStatus() != CommandExecutionStatus.SUCCESS) {
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.FAILED);
    }

    ContextData contextData = buildContextData(context);

    if (!commandStateExecutionData.isDownsize()) {
      buildInstanceStatusSummaries(contextData, response, commandStateExecutionData);
      cleanupOldVersions(contextData);
      return handleNewInstancesAdded(contextData, commandStateExecutionData);
    } else {
      return handleOldInstancesDownsized(contextData, commandStateExecutionData);
    }
  }

  private ExecutionResponse handleNewInstancesAdded(
      ContextData contextData, CommandStateExecutionData commandStateExecutionData) {
    if (contextData.containerServiceElement.getResizeStrategy() == RESIZE_NEW_FIRST) {
      // Done adding new instances, now downsize old instances
      return downsizeOldInstances(contextData, commandStateExecutionData);
    } else {
      // Done with both, return success
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
    }
  }

  private ExecutionResponse handleOldInstancesDownsized(
      ContextData contextData, CommandStateExecutionData commandStateExecutionData) {
    if (contextData.containerServiceElement.getResizeStrategy() == DOWNSIZE_OLD_FIRST) {
      // Done downsizing old instances, now add new instances
      return addNewInstances(contextData, commandStateExecutionData);
    } else {
      // Done with both, return success
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
    }
  }

  private void cleanupOldVersions(ContextData contextData) {
    logger.info("Cleaning up old versions");
    cleanup(contextData.settingAttribute, contextData.region, contextData.containerServiceElement);
  }

  private LinkedHashMap<String, Integer> getOldServiceCounts(ContextData contextData) {
    LinkedHashMap<String, Integer> activeServiceCounts =
        getActiveServiceCounts(contextData.settingAttribute, contextData.region, contextData.containerServiceElement);
    activeServiceCounts.remove(contextData.containerServiceElement.getName());
    return activeServiceCounts;
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback() && getInstanceCount() == 0) {
      invalidFields.put("instanceCount", "instanceCount needs to be greater than 0");
    }
    if (getCommandName() == null) {
      invalidFields.put("commandName", "commandName should not be null");
    }
    return invalidFields;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public abstract int getInstanceCount();

  public abstract InstanceUnitType getInstanceUnitType();

  public abstract String getCommandName();

  protected void cleanup(
      SettingAttribute settingAttribute, String region, ContainerServiceElement containerServiceElement) {}

  protected abstract Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String region, ContainerServiceElement containerServiceElement);

  protected abstract LinkedHashMap<String, Integer> getActiveServiceCounts(
      SettingAttribute settingAttribute, String region, ContainerServiceElement containerServiceElement);

  private ExecutionResponse buildEndStateExecution(
      CommandStateExecutionData commandStateExecutionData, ExecutionStatus status) {
    activityService.updateStatus(
        commandStateExecutionData.getActivityId(), commandStateExecutionData.getAppId(), status);
    InstanceElementListParam instanceElementListParam =
        anInstanceElementListParam()
            .withInstanceElements(Optional
                                      .ofNullable(commandStateExecutionData.getNewInstanceStatusSummaries()
                                                      .stream()
                                                      .map(InstanceStatusSummary::getInstanceElement)
                                                      .collect(Collectors.toList()))
                                      .orElse(emptyList()))
            .build();
    return anExecutionResponse()
        .withStateExecutionData(commandStateExecutionData)
        .withExecutionStatus(status)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  private void buildInstanceStatusSummaries(ContextData contextData, Map<String, NotifyResponseData> response,
      CommandStateExecutionData commandStateExecutionData) {
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService
            .getTemplateRefKeysByService(contextData.appId, contextData.serviceId, contextData.env.getUuid())
            .get(0);

    CommandExecutionData commandExecutionData =
        ((CommandExecutionResult) response.values().iterator().next()).getCommandExecutionData();
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();

    if (commandExecutionData instanceof ResizeCommandUnitExecutionData
        && ((ResizeCommandUnitExecutionData) commandExecutionData).getContainerInfos() != null) {
      ((ResizeCommandUnitExecutionData) commandExecutionData)
          .getContainerInfos()
          .forEach(containerInfo
              -> instanceStatusSummaries.add(
                  anInstanceStatusSummary()
                      .withStatus(containerInfo.getStatus() == ContainerInfo.Status.SUCCESS ? ExecutionStatus.SUCCESS
                                                                                            : ExecutionStatus.FAILED)
                      .withInstanceElement(
                          anInstanceElement()
                              .withUuid(containerInfo.getContainerId())
                              .withHostName(containerInfo.getHostName())
                              .withHostElement(aHostElement().withHostName(containerInfo.getHostName()).build())
                              .withServiceTemplateElement(aServiceTemplateElement()
                                                              .withUuid(serviceTemplateKey.getId().toString())
                                                              .withServiceElement(contextData.serviceElement)
                                                              .build())
                              .withDisplayName(containerInfo.getContainerId())
                              .build())
                      .build()));
    }

    commandStateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);
  }

  private CommandExecutionContext buildCommandExecutionContext(
      ContextData contextData, List<ContainerServiceData> desiredCounts, String activityId) {
    return aCommandExecutionContext()
        .withAccountId(contextData.app.getAccountId())
        .withAppId(contextData.app.getUuid())
        .withEnvId(contextData.env.getUuid())
        .withClusterName(contextData.containerServiceElement.getClusterName())
        .withNamespace(contextData.containerServiceElement.getNamespace())
        .withRegion(contextData.region)
        .withActivityId(activityId)
        .withCloudProviderSetting(contextData.settingAttribute)
        .withDesiredCounts(desiredCounts)
        .build();
  }

  private ContextData buildContextData(ExecutionContext context) {
    return new ContextData(context, this);
  }

  private static class ContextData {
    final Application app;
    final Environment env;
    final Service service;
    final Command command;
    final ServiceElement serviceElement;
    final ContainerServiceElement containerServiceElement;
    final ContainerRollbackRequestElement rollbackElement;
    final SettingAttribute settingAttribute;
    final String appId;
    final String serviceId;
    final String region;
    final String commandUnitName;
    final String infrastructureMappingId;

    ContextData(ExecutionContext context, ContainerServiceDeploy containerServiceDeploy) {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      serviceElement = phaseElement.getServiceElement();
      serviceId = phaseElement.getServiceElement().getUuid();
      appId = context.getAppId();
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      app = workflowStandardParams.getApp();
      env = workflowStandardParams.getEnv();
      service = containerServiceDeploy.serviceResourceService.get(appId, serviceId);
      command = containerServiceDeploy.serviceResourceService
                    .getCommandByName(appId, serviceId, env.getUuid(), containerServiceDeploy.getCommandName())
                    .getCommand();
      InfrastructureMapping infrastructureMapping = containerServiceDeploy.infrastructureMappingService.get(
          workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
      infrastructureMappingId = infrastructureMapping.getUuid();
      settingAttribute = infrastructureMapping instanceof DirectKubernetesInfrastructureMapping
          ? aSettingAttribute()
                .withValue(((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig())
                .build()
          : containerServiceDeploy.settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      region = infrastructureMapping instanceof EcsInfrastructureMapping
          ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
          : "";
      commandUnitName = infrastructureMapping instanceof EcsInfrastructureMapping
          ? CommandUnitType.RESIZE.name()
          : CommandUnitType.RESIZE_KUBERNETES.name();
      containerServiceElement =
          context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
              .stream()
              .filter(cse
                  -> phaseElement.getDeploymentType().equals(cse.getDeploymentType().name())
                      && phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
              .findFirst()
              .orElse(null);
      rollbackElement = context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_ROLLBACK_REQUEST_PARAM);
    }
  }
}
