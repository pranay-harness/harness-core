package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
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
import software.wings.annotation.Encryptable;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InstanceUnitType;
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
import software.wings.beans.command.ContainerResizeParams;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 4/7/17
 */
public abstract class ContainerServiceDeploy extends State {
  private static final Logger logger = LoggerFactory.getLogger(ContainerServiceDeploy.class);

  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private transient DelegateProxyFactory delegateProxyFactory;

  ContainerServiceDeploy(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      logger.info("Executing container service deploy");
      ContextData contextData = buildContextData(context);
      Activity activity = buildActivity(context, contextData);
      CommandStateExecutionData executionData = buildStateExecutionData(contextData, activity.getUuid());

      if (contextData.containerElement.getResizeStrategy() == RESIZE_NEW_FIRST) {
        return addNewInstances(contextData, executionData);
      } else {
        return downsizeOldInstances(contextData, executionData);
      }
    } catch (WingsException e) {
      logger.warn(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  private CommandStateExecutionData buildStateExecutionData(ContextData contextData, String activityId) {
    CommandStateExecutionData.Builder executionDataBuilder =
        aCommandStateExecutionData()
            .withServiceId(contextData.service.getUuid())
            .withServiceName(contextData.service.getName())
            .withAppId(contextData.app.getUuid())
            .withCommandName(getCommandName())
            .withClusterName(contextData.containerElement.getClusterName())
            .withActivityId(activityId);

    if (!isRollback()) {
      logger.info("Executing resize");
      List<ContainerServiceData> newInstanceDataList = new ArrayList<>();
      ContainerServiceData newInstanceData = getNewInstanceData(contextData);
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

  private ContainerServiceData getNewInstanceData(ContextData contextData) {
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(contextData.app.getAccountId()).withAppId(contextData.appId).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(contextData.settingAttribute)
                                                        .containerServiceName(contextData.containerElement.getName())
                                                        .encryptionDetails(contextData.encryptedDataDetails)
                                                        .clusterName(contextData.containerElement.getClusterName())
                                                        .namespace(contextData.containerElement.getNamespace())
                                                        .region(contextData.region)
                                                        .build();
    Optional<Integer> previousDesiredCount = delegateProxyFactory.get(ContainerService.class, syncTaskContext)
                                                 .getServiceDesiredCount(containerServiceParams);

    if (!previousDesiredCount.isPresent()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam("message", "Service setup not done, service name: " + contextData.containerElement.getName());
    }

    int previousCount = previousDesiredCount.get();
    int desiredCount = getNewInstancesDesiredCount(contextData);

    if (desiredCount < previousCount) {
      String msg = "Desired instance count must be greater than or equal to the current instance count: {current: "
          + previousCount + ", desired: " + desiredCount + "}";
      logger.error(msg);
      throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", msg);
    }

    return ContainerServiceData.builder()
        .name(contextData.containerElement.getName())
        .previousCount(previousCount)
        .desiredCount(desiredCount)
        .build();
  }

  private int getNewInstancesDesiredCount(ContextData contextData) {
    if (getInstanceUnitType() == PERCENTAGE) {
      int totalInstancesAvailable;
      if (contextData.containerElement.isUseFixedInstances()) {
        totalInstancesAvailable = contextData.containerElement.getFixedInstances();
      } else {
        SyncTaskContext syncTaskContext =
            aContext().withAccountId(contextData.app.getAccountId()).withAppId(contextData.appId).build();
        ContainerServiceParams containerServiceParams =
            ContainerServiceParams.builder()
                .settingAttribute(contextData.settingAttribute)
                .containerServiceName(contextData.containerElement.getName())
                .encryptionDetails(contextData.encryptedDataDetails)
                .clusterName(contextData.containerElement.getClusterName())
                .namespace(contextData.containerElement.getNamespace())
                .region(contextData.region)
                .build();
        LinkedHashMap<String, Integer> activeServiceCounts =
            delegateProxyFactory.get(ContainerService.class, syncTaskContext)
                .getActiveServiceCounts(containerServiceParams);
        int activeCount = activeServiceCounts.values().stream().mapToInt(Integer::intValue).sum();
        totalInstancesAvailable = activeCount > 0 ? activeCount : contextData.containerElement.getMaxInstances();
      }
      return (int) Math.round(Math.min(getInstanceCount(), 100) * totalInstancesAvailable / 100.0);
    } else {
      if (contextData.containerElement.isUseFixedInstances()) {
        return Math.min(getInstanceCount(), contextData.containerElement.getFixedInstances());
      } else {
        return getInstanceCount();
      }
    }
  }

  private boolean downsizeAllPrevious(ContextData contextData) {
    if (getInstanceUnitType() == PERCENTAGE) {
      return getInstanceCount() >= 100;
    } else {
      return contextData.containerElement.isUseFixedInstances()
          && getInstanceCount() >= contextData.containerElement.getFixedInstances();
    }
  }

  private List<ContainerServiceData> getOldInstanceData(ContextData contextData, ContainerServiceData newServiceData) {
    List<ContainerServiceData> desiredCounts = new ArrayList<>();
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(contextData.app.getAccountId()).withAppId(contextData.appId).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(contextData.settingAttribute)
                                                        .containerServiceName(contextData.containerElement.getName())
                                                        .encryptionDetails(contextData.encryptedDataDetails)
                                                        .clusterName(contextData.containerElement.getClusterName())
                                                        .namespace(contextData.containerElement.getNamespace())
                                                        .region(contextData.region)
                                                        .build();
    LinkedHashMap<String, Integer> previousCounts = delegateProxyFactory.get(ContainerService.class, syncTaskContext)
                                                        .getActiveServiceCounts(containerServiceParams);
    previousCounts.remove(newServiceData.getName());

    int downsizeCount = downsizeAllPrevious(contextData)
        ? previousCounts.values().stream().mapToInt(Integer::intValue).sum()
        : Math.max(newServiceData.getDesiredCount() - newServiceData.getPreviousCount(), 0);

    for (String serviceName : previousCounts.keySet()) {
      int previousCount = previousCounts.get(serviceName);
      int desiredCount = Math.max(previousCount - downsizeCount, 0);
      if (previousCount != desiredCount) {
        desiredCounts.add(ContainerServiceData.builder()
                              .name(serviceName)
                              .previousCount(previousCount)
                              .desiredCount(desiredCount)
                              .build());
      }
      downsizeCount -= previousCount - desiredCount;
    }
    return desiredCounts;
  }

  private ExecutionResponse addNewInstances(ContextData contextData, CommandStateExecutionData executionData) {
    List<ContainerServiceData> desiredCounts = executionData.getNewInstanceData();
    if (isEmpty(desiredCounts)) {
      // No instances to add; continue execution. This happens on rollback of a first deployment.
      return handleNewInstancesAdded(contextData, executionData);
    }
    executionData.setDownsize(false);
    logger.info("Adding instances for {} services", desiredCounts.size());
    return queueResizeTask(contextData, executionData, desiredCounts);
  }

  private ExecutionResponse downsizeOldInstances(ContextData contextData, CommandStateExecutionData executionData) {
    List<ContainerServiceData> desiredCounts = executionData.getOldInstanceData();
    if (isEmpty(desiredCounts)) {
      // No instances to downsize; continue execution. This happens on a first deployment.
      return handleOldInstancesDownsized(contextData, executionData);
    }
    executionData.setDownsize(true);
    logger.info("Downsizing {} services", desiredCounts.size());
    return queueResizeTask(contextData, executionData, desiredCounts);
  }

  private ExecutionResponse queueResizeTask(
      ContextData contextData, CommandStateExecutionData executionData, List<ContainerServiceData> desiredCounts) {
    desiredCounts.forEach(dc
        -> logger.info("Changing desired count for service {} from {} to {}", dc.getName(), dc.getPreviousCount(),
            dc.getDesiredCount()));
    CommandExecutionContext commandExecutionContext =
        buildCommandExecutionContext(contextData, desiredCounts, executionData.getActivityId());

    String waitId = UUID.randomUUID().toString();
    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(contextData.app.getAccountId())
                                      .withAppId(contextData.appId)
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(waitId)
                                      .withParameters(new Object[] {contextData.command, commandExecutionContext})
                                      .withEnvId(contextData.env.getUuid())
                                      .withInfrastructureMappingId(contextData.infrastructureMappingId)
                                      .withTimeout(TimeUnit.HOURS.toMillis(1))
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(waitId))
        .withStateExecutionData(executionData)
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      logger.info("Received async response");
      CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
      CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();

      if (commandExecutionResult == null || commandExecutionResult.getStatus() != CommandExecutionStatus.SUCCESS) {
        return buildEndStateExecution(executionData, ExecutionStatus.FAILED);
      }

      ContextData contextData = buildContextData(context);

      if (!executionData.isDownsize()) {
        executionData.setNewInstanceStatusSummaries(buildInstanceStatusSummaries(contextData, response));
        return handleNewInstancesAdded(contextData, executionData);
      } else {
        return handleOldInstancesDownsized(contextData, executionData);
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  private ExecutionResponse handleNewInstancesAdded(ContextData contextData, CommandStateExecutionData executionData) {
    if (contextData.containerElement.getResizeStrategy() == RESIZE_NEW_FIRST) {
      // Done adding new instances, now downsize old instances
      return downsizeOldInstances(contextData, executionData);
    } else {
      return buildEndStateExecution(executionData, ExecutionStatus.SUCCESS);
    }
  }

  private ExecutionResponse handleOldInstancesDownsized(
      ContextData contextData, CommandStateExecutionData executionData) {
    if (contextData.containerElement.getResizeStrategy() == DOWNSIZE_OLD_FIRST) {
      // Done downsizing old instances, now add new instances
      return addNewInstances(contextData, executionData);
    } else {
      return buildEndStateExecution(executionData, ExecutionStatus.SUCCESS);
    }
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback() && getInstanceCount() == 0) {
      invalidFields.put("instanceCount", "Instance count must be greater than 0");
    }
    if (getCommandName() == null) {
      invalidFields.put("commandName", "Command name must not be null");
    }
    return invalidFields;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public abstract int getInstanceCount();

  public abstract InstanceUnitType getInstanceUnitType();

  public abstract String getCommandName();

  protected abstract ContainerResizeParams buildContainerResizeParams(
      ContextData contextData, List<ContainerServiceData> desiredCounts);

  private Activity buildActivity(ExecutionContext context, ContextData contextData) {
    Activity activity = Activity.builder()
                            .applicationName(contextData.app.getName())
                            .environmentId(contextData.env.getUuid())
                            .environmentName(contextData.env.getName())
                            .environmentType(contextData.env.getEnvironmentType())
                            .serviceId(contextData.service.getUuid())
                            .serviceName(contextData.service.getName())
                            .commandName(contextData.command.getName())
                            .type(Type.Command)
                            .workflowExecutionId(context.getWorkflowExecutionId())
                            .workflowType(context.getWorkflowType())
                            .workflowId(context.getWorkflowId())
                            .workflowExecutionName(context.getWorkflowExecutionName())
                            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                            .commandUnits(serviceResourceService.getFlattenCommandUnitList(contextData.app.getUuid(),
                                contextData.serviceId, contextData.env.getUuid(), contextData.command.getName()))
                            .commandType(contextData.command.getCommandUnitType().name())
                            .serviceVariables(context.getServiceVariables())
                            .status(ExecutionStatus.RUNNING)
                            .build();

    activity.setAppId(contextData.appId);
    return activityService.save(activity);
  }

  private ExecutionResponse buildEndStateExecution(CommandStateExecutionData executionData, ExecutionStatus status) {
    activityService.updateStatus(executionData.getActivityId(), executionData.getAppId(), status);
    InstanceElementListParam instanceElementListParam =
        anInstanceElementListParam()
            .withInstanceElements(Optional
                                      .ofNullable(executionData.getNewInstanceStatusSummaries()
                                                      .stream()
                                                      .map(InstanceStatusSummary::getInstanceElement)
                                                      .collect(Collectors.toList()))
                                      .orElse(emptyList()))
            .build();
    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(status)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  private List<InstanceStatusSummary> buildInstanceStatusSummaries(
      ContextData contextData, Map<String, NotifyResponseData> response) {
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
                              .withDockerId(containerInfo.getContainerId())
                              .withHostName(containerInfo.getHostName())
                              .withHost(aHostElement()
                                            .withHostName(containerInfo.getHostName())
                                            .withEc2Instance(containerInfo.getEc2Instance())
                                            .build())
                              .withServiceTemplateElement(aServiceTemplateElement()
                                                              .withUuid(serviceTemplateKey.getId().toString())
                                                              .withServiceElement(contextData.serviceElement)
                                                              .build())
                              .withDisplayName(containerInfo.getContainerId())
                              .build())
                      .build()));
    }
    return instanceStatusSummaries;
  }

  private CommandExecutionContext buildCommandExecutionContext(
      ContextData contextData, List<ContainerServiceData> desiredCounts, String activityId) {
    ContainerResizeParams params = buildContainerResizeParams(contextData, desiredCounts);
    return aCommandExecutionContext()
        .withAccountId(contextData.app.getAccountId())
        .withAppId(contextData.app.getUuid())
        .withEnvId(contextData.env.getUuid())
        .withActivityId(activityId)
        .withCloudProviderSetting(contextData.settingAttribute)
        .withCloudProviderCredentials(contextData.encryptedDataDetails)
        .withContainerResizeParams(params)
        .build();
  }

  private ContextData buildContextData(ExecutionContext context) {
    return new ContextData(context, this);
  }

  protected static class ContextData {
    final Application app;
    final Environment env;
    final Service service;
    final Command command;
    final ServiceElement serviceElement;
    final ContainerServiceElement containerElement;
    final ContainerRollbackRequestElement rollbackElement;
    final SettingAttribute settingAttribute;
    final List<EncryptedDataDetail> encryptedDataDetails;
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
      encryptedDataDetails = containerServiceDeploy.secretManager.getEncryptionDetails(
          (Encryptable) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
      region = infrastructureMapping instanceof EcsInfrastructureMapping
          ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
          : "";
      commandUnitName = infrastructureMapping instanceof EcsInfrastructureMapping
          ? CommandUnitType.RESIZE.name()
          : CommandUnitType.RESIZE_KUBERNETES.name();
      containerElement = context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
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
