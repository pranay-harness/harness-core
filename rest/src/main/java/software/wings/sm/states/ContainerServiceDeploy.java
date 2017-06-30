package software.wings.sm.states;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
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
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionData;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
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

  ContainerServiceDeploy(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, env.getUuid(), getCommandName()).getCommand();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = getSettingAttribute(infrastructureMapping);

    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    String clusterName = serviceElement.getClusterName();
    String serviceName = serviceElement.getName();
    String region = "";
    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      region = ((EcsInfrastructureMapping) infrastructureMapping).getRegion();
    }

    executionDataBuilder.withServiceId(service.getUuid())
        .withServiceName(service.getName())
        .withAppId(app.getUuid())
        .withCommandName(getCommandName())
        .withClusterName(clusterName);

    Activity.Builder activityBuilder = anActivity()
                                           .withAppId(app.getUuid())
                                           .withApplicationName(app.getName())
                                           .withEnvironmentId(env.getUuid())
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
                                               app.getUuid(), serviceId, env.getUuid(), command.getName()))
                                           .withCommandType(command.getCommandUnitType().name())
                                           .withServiceVariables(context.getServiceVariables());

    Activity activity = activityService.save(activityBuilder.build());
    executionDataBuilder.withActivityId(activity.getUuid()).withNewContainerServiceName(serviceName);

    int desiredCount;
    if (isRollback()) {
      ContainerUpgradeRequestElement containerUpgradeRequestElement =
          context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_UPGRADE_REQUEST_PARAM);
      desiredCount = containerUpgradeRequestElement.getNewServiceInstanceCount();
      if (desiredCount == 0) {
        // This is a rollback of a deployment where the new version was the first one, so there isn't an old one to
        // scale up during rollback. We just need to downsize the new one (which is called 'old' during rollback
        // execution)
        return downsizeOldInstances(context, executionDataBuilder.build());
      }
    } else {
      Optional<Integer> previousDesiredCount =
          getServiceDesiredCount(settingAttribute, region, clusterName, serviceName);
      if (!previousDesiredCount.isPresent()) {
        throw new WingsException(
            ErrorCode.INVALID_REQUEST, "message", "Service setup not done, serviceName: " + serviceName);
      }
      executionDataBuilder.withNewServicePreviousInstanceCount(previousDesiredCount.get());
      desiredCount = previousDesiredCount.get() + fetchDesiredCount();
    }

    logger.info("Desired count for service {} is {}", serviceName, desiredCount);

    CommandExecutionContext commandExecutionContext = buildCommandExecutionContext(
        app, env.getUuid(), clusterName, serviceName, region, desiredCount, activity.getUuid(), settingAttribute);

    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(app.getAccountId())
                                      .withAppId(app.getAppId())
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(activity.getUuid())
                                      .withParameters(new Object[] {command, commandExecutionContext})
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(activity.getUuid()))
        .withStateExecutionData(executionDataBuilder.build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private SettingAttribute getSettingAttribute(InfrastructureMapping infrastructureMapping) {
    SettingAttribute settingAttribute;
    if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
      settingAttribute =
          aSettingAttribute()
              .withValue(((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig())
              .build();
    } else {
      settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    }
    return settingAttribute;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();

    CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();
    if (commandExecutionResult == null || commandExecutionResult.getStatus() != CommandExecutionStatus.SUCCESS) {
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.FAILED);
    }
    if (commandStateExecutionData.getOldContainerServiceName() == null) {
      buildInstanceStatusSummaries(context, response, commandStateExecutionData);
      cleanupOldVersions(context);
      return downsizeOldInstances(context, commandStateExecutionData);
    } else {
      CommandExecutionData commandExecutionData = commandExecutionResult.getCommandExecutionData();
      if (commandExecutionData instanceof ResizeCommandUnitExecutionData) {
        int actualOldInstanceCount =
            ((ResizeCommandUnitExecutionData) commandExecutionData)
                .getContainerInfos()
                .stream()
                .filter(containerInfo -> containerInfo.getStatus() == ContainerInfo.Status.SUCCESS)
                .collect(Collectors.toList())
                .size();
        commandStateExecutionData.setOldServiceRunningInstanceCount(actualOldInstanceCount);
      }
      return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
    }
  }

  private ExecutionResponse downsizeOldInstances(
      ExecutionContext context, CommandStateExecutionData commandStateExecutionData) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = getSettingAttribute(infrastructureMapping);

    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    String clusterName = serviceElement.getClusterName();
    String oldServiceName = serviceElement.getOldName();
    String region = "";
    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      region = ((EcsInfrastructureMapping) infrastructureMapping).getRegion();
    }

    commandStateExecutionData.setOldContainerServiceName(oldServiceName);

    int desiredCount;

    if (isRollback()) {
      ContainerUpgradeRequestElement containerUpgradeRequestElement =
          context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_UPGRADE_REQUEST_PARAM);
      desiredCount = containerUpgradeRequestElement.getOldServiceInstanceCount();
    } else {
      Optional<Integer> previousDesiredCount =
          getServiceDesiredCount(settingAttribute, region, clusterName, oldServiceName);
      if (!previousDesiredCount.isPresent()) {
        // Old service doesn't exist so we don't need to do anything
        return buildEndStateExecution(commandStateExecutionData, ExecutionStatus.SUCCESS);
      }
      commandStateExecutionData.setOldServicePreviousInstanceCount(previousDesiredCount.get());
      desiredCount = Math.max(previousDesiredCount.get() - fetchDesiredCount(), 0);
    }

    logger.info("Desired count for service {} is {}", oldServiceName, desiredCount);

    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    Command command = serviceResourceService
                          .getCommandByName(workflowStandardParams.getAppId(),
                              phaseElement.getServiceElement().getUuid(), env.getUuid(), getCommandName())
                          .getCommand();

    CommandExecutionContext commandExecutionContext = buildCommandExecutionContext(app, env.getUuid(), clusterName,
        oldServiceName, region, desiredCount, commandStateExecutionData.getActivityId(), settingAttribute);

    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(app.getAccountId())
                                      .withAppId(app.getAppId())
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(commandStateExecutionData.getActivityId())
                                      .withParameters(new Object[] {command, commandExecutionContext})
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(commandStateExecutionData.getActivityId()))
        .withStateExecutionData(commandStateExecutionData)
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  private void cleanupOldVersions(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(workflowStandardParams.getAppId(), phaseElement.getInfraMappingId());
    SettingAttribute settingAttribute = getSettingAttribute(infrastructureMapping);
    ContainerServiceElement serviceElement = getContainerServiceElement(context);
    String serviceName = serviceElement.getName();
    String clusterName = serviceElement.getClusterName();
    String region = "";
    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      region = ((EcsInfrastructureMapping) infrastructureMapping).getRegion();
    }
    cleanup(settingAttribute, region, clusterName, serviceName);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback() && fetchDesiredCount() == 0) {
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
    Environment env = workflowStandardParams.getEnv();
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, env.getUuid()).get(0);

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
    commandStateExecutionData.setNewServiceRunningInstanceCount(
        (int) instanceStatusSummaries.stream()
            .filter(instanceStatusSummary -> instanceStatusSummary.getStatus() == ExecutionStatus.SUCCESS)
            .count());
  }

  private CommandExecutionContext buildCommandExecutionContext(Application app, String envId, String clusterName,
      String serviceName, String region, int desiredCount, String activityId, SettingAttribute settingAttribute) {
    CommandExecutionContext commandExecutionContext =
        aCommandExecutionContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).withEnvId(envId).build();
    commandExecutionContext.setClusterName(clusterName);
    commandExecutionContext.setServiceName(serviceName);
    commandExecutionContext.setRegion(region);
    commandExecutionContext.setActivityId(activityId);
    commandExecutionContext.setCloudProviderSetting(settingAttribute);
    commandExecutionContext.setDesiredCount(desiredCount);

    return commandExecutionContext;
  }

  public abstract int fetchDesiredCount();

  public abstract String getCommandName();

  protected abstract Optional<Integer> getServiceDesiredCount(
      SettingAttribute settingAttribute, String region, String clusterName, @Nullable String serviceName);

  protected void cleanup(SettingAttribute settingAttribute, String region, String clusterName, String serviceName) {}

  private ContainerServiceElement getContainerServiceElement(ExecutionContext context) {
    if (isRollback()) {
      ContainerUpgradeRequestElement upgradeElement =
          context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_UPGRADE_REQUEST_PARAM);
      return upgradeElement.getContainerServiceElement();
    } else {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      List<ContainerServiceElement> containerServiceElements =
          context.getContextElementList(ContextElementType.CONTAINER_SERVICE);

      return containerServiceElements.stream()
          .filter(containerServiceElement
              -> phaseElement.getDeploymentType().equals(containerServiceElement.getDeploymentType().name())
                  && phaseElement.getInfraMappingId().equals(containerServiceElement.getInfraMappingId()))
          .findFirst()
          .orElse(null);
    }
  }
}
