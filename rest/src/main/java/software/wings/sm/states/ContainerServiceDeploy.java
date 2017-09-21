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
import software.wings.api.ContainerServiceData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerUpgradeRequestElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
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
import javax.annotation.Nullable;

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

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    String envId = env.getUuid();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, envId, getCommandName()).getCommand();

    Activity.Builder activityBuilder = anActivity()
                                           .withAppId(app.getUuid())
                                           .withApplicationName(app.getName())
                                           .withEnvironmentId(envId)
                                           .withEnvironmentName(env.getName())
                                           .withEnvironmentType(env.getEnvironmentType())
                                           .withServiceId(service.getUuid())
                                           .withServiceName(service.getName())
                                           .withCommandName(command.getName())
                                           .withType(Type.Command)
                                           .withWorkflowExecutionId(context.getWorkflowExecutionId())
                                           .withWorkflowType(context.getWorkflowType())
                                           .withWorkflowExecutionName(context.getWorkflowExecutionName())
                                           .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                           .withStateExecutionInstanceName(context.getStateExecutionInstanceName())
                                           .withCommandUnits(serviceResourceService.getFlattenCommandUnitList(
                                               app.getUuid(), serviceId, envId, command.getName()))
                                           .withCommandType(command.getCommandUnitType().name())
                                           .withServiceVariables(context.getServiceVariables());

    String activityId = activityService.save(activityBuilder.build()).getUuid();

    ContainerServiceElement serviceElement = getContainerServiceElement(context);

    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData()
                                                                 .withServiceId(service.getUuid())
                                                                 .withServiceName(service.getName())
                                                                 .withAppId(app.getUuid())
                                                                 .withCommandName(getCommandName())
                                                                 .withClusterName(serviceElement.getClusterName())
                                                                 .withActivityId(activityId);

    if (!isRollback()) {
      logger.info("Executing resize");
      List<ContainerServiceData> desiredCountsNew = new ArrayList<>();
      ContainerServiceData newInstanceData = getNewInstanceData(context, activityId);
      desiredCountsNew.add(newInstanceData);
      executionDataBuilder.withNewInstanceData(desiredCountsNew);
      executionDataBuilder.withOldInstanceData(getOldInstanceData(context, newInstanceData));
      executionDataBuilder.withResizeStrategy(serviceElement.getResizeStrategy());
    } else {
      logger.info("Executing rollback");
      ContainerUpgradeRequestElement containerUpgradeRequestElement =
          context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_UPGRADE_REQUEST_PARAM);
      executionDataBuilder.withNewInstanceData(containerUpgradeRequestElement.getNewInstanceData());
      executionDataBuilder.withOldInstanceData(containerUpgradeRequestElement.getOldInstanceData());
      executionDataBuilder.withResizeStrategy(containerUpgradeRequestElement.getResizeStrategy());
    }

    CommandStateExecutionData commandStateExecutionData = executionDataBuilder.build();
    if (commandStateExecutionData.getResizeStrategy() == RESIZE_NEW_FIRST) {
      return resizeNewInstances(context, commandStateExecutionData);
    } else {
      return downsizeOldInstances(context, commandStateExecutionData);
    }
  }

  private ContainerServiceData getNewInstanceData(ExecutionContext context, String activityId) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(workflowStandardParams.getApp().getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = getSettingAttribute(infrastructureMapping);
    String region = infrastructureMapping instanceof EcsInfrastructureMapping
        ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
        : "";
    String newContainerServiceName = serviceElement.getName();
    Optional<Integer> previousDesiredCount =
        getServiceDesiredCount(settingAttribute, region, serviceElement.getClusterName(), newContainerServiceName);
    if (!previousDesiredCount.isPresent()) {
      throw new WingsException(
          ErrorCode.INVALID_REQUEST, "message", "Service setup not done, serviceName: " + newContainerServiceName);
    }
    int previousCountNew = previousDesiredCount.get();
    int desiredCountNew = fetchDesiredCount(serviceElement.getMaxInstances());

    if (desiredCountNew <= previousCountNew) {
      String msg = "Desired instance count is less than or equal to the current instance count: {current: "
          + previousCountNew + ", desired: " + desiredCountNew + "}";
      logger.warn(msg);
      logService.save(aLog()
                          .withAppId(context.getAppId())
                          .withActivityId(activityId)
                          .withCommandUnitName(infrastructureMapping instanceof EcsInfrastructureMapping
                                  ? CommandUnitType.RESIZE.name()
                                  : CommandUnitType.RESIZE_KUBERNETES.name())
                          .withLogLine(msg)
                          .withLogLevel(Log.LogLevel.WARN)
                          .build());
    }

    if (desiredCountNew > serviceElement.getMaxInstances()) {
      String msg = "Desired instance count is greater than the maximum instance count: {maximum: "
          + serviceElement.getMaxInstances() + ", desired: " + desiredCountNew + "}";
      logger.error(msg);
      logService.save(aLog()
                          .withAppId(context.getAppId())
                          .withActivityId(activityId)
                          .withCommandUnitName(infrastructureMapping instanceof EcsInfrastructureMapping
                                  ? CommandUnitType.RESIZE.name()
                                  : CommandUnitType.RESIZE_KUBERNETES.name())
                          .withLogLine(msg)
                          .withLogLevel(Log.LogLevel.WARN)
                          .build());
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", msg);
    }

    return aContainerServiceData()
        .withName(newContainerServiceName)
        .withPreviousCount(previousCountNew)
        .withDesiredCount(desiredCountNew)
        .build();
  }

  private List<ContainerServiceData> getOldInstanceData(ExecutionContext context, ContainerServiceData newServiceData) {
    List<ContainerServiceData> desiredCountsOld = new ArrayList<>();
    LinkedHashMap<String, Integer> oldServiceCounts = getOldServiceCounts(context);
    int downsizeCount = Math.max(newServiceData.getDesiredCount() - newServiceData.getPreviousCount(), 0);
    for (String serviceName : oldServiceCounts.keySet()) {
      int previousCount = oldServiceCounts.get(serviceName);
      int desiredCount = Math.max(previousCount - downsizeCount, 0);
      if (previousCount != desiredCount) {
        desiredCountsOld.add(aContainerServiceData()
                                 .withName(serviceName)
                                 .withPreviousCount(previousCount)
                                 .withDesiredCount(desiredCount)
                                 .build());
      }
      downsizeCount -= previousCount - desiredCount;
    }
    return desiredCountsOld;
  }

  private ExecutionResponse resizeNewInstances(
      ExecutionContext context, CommandStateExecutionData commandStateExecutionData) {
    List<ContainerServiceData> desiredCounts = commandStateExecutionData.getNewInstanceData();
    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    commandStateExecutionData.setDownsize(false);
    if (desiredCounts == null || desiredCounts.isEmpty()) {
      // This is a rollback of a deployment where the new version was the first one, so there isn't an old one to scale
      // up during rollback. We just need to downsize the new one (which is called 'old' during rollback execution)
      return handleResizeNewComplete(context, commandStateExecutionData);
    }
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    String envId = workflowStandardParams.getEnv().getUuid();
    String serviceId = phaseElement.getServiceElement().getUuid();
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = getSettingAttribute(infrastructureMapping);
    String region = infrastructureMapping instanceof EcsInfrastructureMapping
        ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
        : "";
    String activityId = commandStateExecutionData.getActivityId();
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, envId, getCommandName()).getCommand();

    logger.info("Setting desired count for {} services", desiredCounts.size());
    desiredCounts.forEach(dc
        -> logger.info("Changing desired count for service {} from {} to {}", dc.getName(), dc.getPreviousCount(),
            dc.getDesiredCount()));

    CommandExecutionContext commandExecutionContext = buildCommandExecutionContext(
        app, envId, serviceElement.getClusterName(), region, desiredCounts, activityId, settingAttribute);

    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(app.getAccountId())
                                      .withAppId(app.getAppId())
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(activityId)
                                      .withParameters(new Object[] {command, commandExecutionContext})
                                      .withEnvId(envId)
                                      .withInfrastructureMappingId(infrastructureMapping.getUuid())
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(activityId))
        .withStateExecutionData(commandStateExecutionData)
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private ExecutionResponse downsizeOldInstances(
      ExecutionContext context, CommandStateExecutionData commandStateExecutionData) {
    List<ContainerServiceData> desiredCounts = commandStateExecutionData.getOldInstanceData();
    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    commandStateExecutionData.setDownsize(true);
    if (desiredCounts == null || desiredCounts.isEmpty()) {
      // Old service doesn't exist so we don't need to do anything
      return handleDownsizeOldComplete(context, commandStateExecutionData);
    }
    logger.info("Downsizing old instances");
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    String envId = workflowStandardParams.getEnv().getUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = getSettingAttribute(infrastructureMapping);
    String clusterName = serviceElement.getClusterName();
    String region = infrastructureMapping instanceof EcsInfrastructureMapping
        ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
        : "";

    logger.info("Downsizing {} services", desiredCounts.size());
    desiredCounts.forEach(dc
        -> logger.info("Changing desired count for service {} from {} to {}", dc.getName(), dc.getPreviousCount(),
            dc.getDesiredCount()));

    Command command = serviceResourceService
                          .getCommandByName(workflowStandardParams.getAppId(),
                              phaseElement.getServiceElement().getUuid(), envId, getCommandName())
                          .getCommand();

    CommandExecutionContext commandExecutionContext = buildCommandExecutionContext(
        app, envId, clusterName, region, desiredCounts, commandStateExecutionData.getActivityId(), settingAttribute);

    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(app.getAccountId())
                                      .withAppId(app.getAppId())
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(commandStateExecutionData.getActivityId())
                                      .withParameters(new Object[] {command, commandExecutionContext})
                                      .withEnvId(envId)
                                      .withInfrastructureMappingId(infrastructureMapping.getUuid())
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
    if (!commandStateExecutionData.isDownsize()) {
      buildInstanceStatusSummaries(context, response, commandStateExecutionData);
      cleanupOldVersions(context);
      return handleResizeNewComplete(context, commandStateExecutionData);
    } else {
      return handleDownsizeOldComplete(context, commandStateExecutionData);
    }
  }

  private ExecutionResponse handleResizeNewComplete(
      ExecutionContext context, CommandStateExecutionData commandStateExecutionData) {
    if (commandStateExecutionData.getResizeStrategy() == RESIZE_NEW_FIRST) {
      // Done with resize new, now downsize old
      return downsizeOldInstances(context, commandStateExecutionData);
    } else {
      // Done with everything, return success
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
    }
  }

  private ExecutionResponse handleDownsizeOldComplete(
      ExecutionContext context, CommandStateExecutionData commandStateExecutionData) {
    if (commandStateExecutionData.getResizeStrategy() == DOWNSIZE_OLD_FIRST) {
      // Done with downsize old, now resize new
      return resizeNewInstances(context, commandStateExecutionData);
    } else {
      // Done with everything, return success
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
    }
  }

  private void cleanupOldVersions(ExecutionContext context) {
    logger.info("Cleaning up old versions");
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    String region = infrastructureMapping instanceof EcsInfrastructureMapping
        ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
        : "";
    cleanup(
        getSettingAttribute(infrastructureMapping), region, serviceElement.getClusterName(), serviceElement.getName());
  }

  private LinkedHashMap<String, Integer> getOldServiceCounts(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    String region = infrastructureMapping instanceof EcsInfrastructureMapping
        ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
        : "";
    LinkedHashMap<String, Integer> activeServiceCounts = getActiveServiceCounts(
        getSettingAttribute(infrastructureMapping), region, serviceElement.getClusterName(), serviceElement.getName());
    activeServiceCounts.remove(serviceElement.getName());
    return activeServiceCounts;
  }

  private SettingAttribute getSettingAttribute(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof DirectKubernetesInfrastructureMapping
        ? aSettingAttribute()
              .withValue(((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig())
              .build()
        : settingsService.get(infrastructureMapping.getComputeProviderSettingId());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public abstract int getInstanceCount();

  public abstract InstanceUnitType getInstanceUnitType();

  private int fetchDesiredCount(int maxInstances) {
    if (getInstanceUnitType() == InstanceUnitType.PERCENTAGE) {
      return Math.max((Math.min(getInstanceCount(), 100) * maxInstances) / 100, 1);
    } else {
      return Math.min(getInstanceCount(), maxInstances);
    }
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

  private void buildInstanceStatusSummaries(ExecutionContext context, Map<String, NotifyResponseData> response,
      CommandStateExecutionData commandStateExecutionData) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    String envId = workflowStandardParams.getEnv().getUuid();
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, envId).get(0);

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
                                                              .withServiceElement(phaseElement.getServiceElement())
                                                              .build())
                              .withDisplayName(containerInfo.getContainerId())
                              .build())
                      .build()));
    }

    commandStateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);
  }

  private CommandExecutionContext buildCommandExecutionContext(Application app, String envId, String clusterName,
      String region, List<ContainerServiceData> desiredCounts, String activityId, SettingAttribute settingAttribute) {
    return aCommandExecutionContext()
        .withAccountId(app.getAccountId())
        .withAppId(app.getUuid())
        .withEnvId(envId)
        .withClusterName(clusterName)
        .withRegion(region)
        .withActivityId(activityId)
        .withCloudProviderSetting(settingAttribute)
        .withDesiredCounts(desiredCounts)
        .build();
  }

  public abstract String getCommandName();

  protected abstract Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String region, String clusterName, @Nullable String serviceName);

  protected abstract LinkedHashMap<String, Integer> getActiveServiceCounts(
      SettingAttribute settingAttribute, String region, String clusterName, String serviceName);

  protected void cleanup(SettingAttribute settingAttribute, String region, String clusterName, String serviceName) {}

  private ContainerServiceElement getContainerServiceElement(ExecutionContext context) {
    if (isRollback()) {
      ContainerUpgradeRequestElement upgradeElement =
          context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_UPGRADE_REQUEST_PARAM);
      return upgradeElement.getContainerServiceElement();
    } else {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      return context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
          .stream()
          .filter(containerServiceElement
              -> phaseElement.getDeploymentType().equals(containerServiceElement.getDeploymentType().name())
                  && phaseElement.getInfraMappingId().equals(containerServiceElement.getInfraMappingId()))
          .findFirst()
          .orElse(null);
    }
  }
}
