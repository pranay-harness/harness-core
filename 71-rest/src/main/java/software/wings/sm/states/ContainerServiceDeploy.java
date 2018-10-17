package software.wings.sm.states;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
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
import software.wings.beans.command.ContainerResizeParams;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.common.Constants;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class ContainerServiceDeploy extends State {
  private static final Logger logger = LoggerFactory.getLogger(ContainerServiceDeploy.class);

  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private transient ContainerDeploymentManagerHelper containerDeploymentHelper;
  @Inject @Transient protected transient FeatureFlagService featureFlagService;

  ContainerServiceDeploy(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      logger.info("Executing container service deploy");
      ContextData contextData = new ContextData(context, this);

      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(contextData.appId, contextData.infrastructureMappingId);

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
                              .status(ExecutionStatus.RUNNING)
                              .build();

      activity.setAppId(contextData.appId);
      activity = activityService.save(activity);

      CommandStateExecutionData.Builder executionDataBuilder =
          aCommandStateExecutionData()
              .withServiceId(contextData.service.getUuid())
              .withServiceName(contextData.service.getName())
              .withAppId(contextData.app.getUuid())
              .withCommandName(getCommandName())
              .withClusterName(contextData.containerElement.getClusterName())
              .withActivityId(activity.getUuid());

      if (isRollback()) {
        logger.info("Executing rollback");
        executionDataBuilder.withNewInstanceData(contextData.rollbackElement.getNewInstanceData());
        executionDataBuilder.withOldInstanceData(contextData.rollbackElement.getOldInstanceData());
      }

      CommandStateExecutionData executionData = executionDataBuilder.build();

      ContainerResizeParams params = buildContainerResizeParams(context, contextData);
      CommandExecutionContext commandExecutionContext =
          aCommandExecutionContext()
              .withAccountId(contextData.app.getAccountId())
              .withAppId(contextData.app.getUuid())
              .withEnvId(contextData.env.getUuid())
              .withActivityId(activity.getUuid())
              .withCloudProviderSetting(contextData.settingAttribute)
              .withCloudProviderCredentials(contextData.encryptedDataDetails)
              .withContainerResizeParams(params)
              .withDeploymentType(infrastructureMapping.getDeploymentType())
              .build();

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
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      logger.info("Received async response");
      CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
      CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();

      if (commandExecutionResult == null || commandExecutionResult.getStatus() != CommandExecutionStatus.SUCCESS) {
        return buildEndStateExecution(executionData, commandExecutionResult, ExecutionStatus.FAILED);
      }

      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      ServiceElement serviceElement = phaseElement.getServiceElement();
      String serviceId = phaseElement.getServiceElement().getUuid();
      String appId = context.getAppId();
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String envId = workflowStandardParams.getEnv().getUuid();
      executionData.setNewInstanceStatusSummaries(
          buildInstanceStatusSummaries(appId, serviceId, envId, serviceElement, response));
      return buildEndStateExecution(executionData, commandExecutionResult, ExecutionStatus.SUCCESS);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback() && isBlank(getInstanceCount())) {
      invalidFields.put("instanceCount", "Instance count must not be blank");
    }
    if (isBlank(getCommandName())) {
      invalidFields.put("commandName", "Command name must not be blank");
    }
    return invalidFields;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public abstract String getInstanceCount();

  public abstract InstanceUnitType getInstanceUnitType();

  public abstract String getDownsizeInstanceCount();

  public abstract InstanceUnitType getDownsizeInstanceUnitType();

  public abstract String getCommandName();

  protected abstract ContainerResizeParams buildContainerResizeParams(
      ExecutionContext context, ContextData contextData);

  private ExecutionResponse buildEndStateExecution(
      CommandStateExecutionData executionData, CommandExecutionResult executionResult, ExecutionStatus status) {
    activityService.updateStatus(executionData.getActivityId(), executionData.getAppId(), status);

    List<InstanceElement> instanceElements = executionData.getNewInstanceStatusSummaries()
                                                 .stream()
                                                 .map(InstanceStatusSummary::getInstanceElement)
                                                 .collect(toList());
    InstanceElementListParam listParam = anInstanceElementListParam().withInstanceElements(instanceElements).build();

    if (executionResult != null) {
      ResizeCommandUnitExecutionData resizeExecutionData =
          (ResizeCommandUnitExecutionData) executionResult.getCommandExecutionData();
      if (resizeExecutionData != null) {
        executionData.setNewInstanceData(resizeExecutionData.getNewInstanceData());
        executionData.setOldInstanceData(resizeExecutionData.getOldInstanceData());
      }
      executionData.setDelegateMetaInfo(executionResult.getDelegateMetaInfo());
    }

    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(status)
        .addContextElement(listParam)
        .addNotifyElement(listParam)
        .build();
  }

  private List<InstanceStatusSummary> buildInstanceStatusSummaries(
      String appId, String serviceId, String envId, ServiceElement serviceElement, Map<String, ResponseData> response) {
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0);
    CommandExecutionData commandExecutionData =
        ((CommandExecutionResult) response.values().iterator().next()).getCommandExecutionData();
    ResizeCommandUnitExecutionData resizeExecutionData = (ResizeCommandUnitExecutionData) commandExecutionData;
    ServiceTemplateElement serviceTemplateElement = aServiceTemplateElement()
                                                        .withUuid(serviceTemplateKey.getId().toString())
                                                        .withServiceElement(serviceElement)
                                                        .build();
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    if (resizeExecutionData != null) {
      instanceStatusSummaries.addAll(containerDeploymentHelper.getInstanceStatusSummaryFromContainerInfoList(
          resizeExecutionData.getContainerInfos(), serviceTemplateElement));
    }
    return instanceStatusSummaries;
  }

  protected static class ContextData {
    final Application app;
    final Environment env;
    final Service service;
    final Command command;
    final ContainerServiceElement containerElement;
    final ContainerRollbackRequestElement rollbackElement;
    final SettingAttribute settingAttribute;
    final List<EncryptedDataDetail> encryptedDataDetails;
    final String appId;
    final String serviceId;
    final String region;
    final String infrastructureMappingId;
    final String subscriptionId;
    final String resourceGroup;
    final Integer instanceCount;
    final Integer downsizeInstanceCount;

    ContextData(ExecutionContext context, ContainerServiceDeploy containerServiceDeploy) {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
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
              && infrastructureMapping.getComputeProviderType().equals(SettingVariableTypes.DIRECT.name())
          ? aSettingAttribute()
                .withValue(((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig())
                .build()
          : containerServiceDeploy.settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      encryptedDataDetails = containerServiceDeploy.secretManager.getEncryptionDetails(
          (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
      region = infrastructureMapping instanceof EcsInfrastructureMapping
          ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
          : "";
      containerElement = context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
                             .stream()
                             .filter(cse -> phaseElement.getDeploymentType().equals(cse.getDeploymentType().name()))
                             .filter(cse -> phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
                             .findFirst()
                             .orElse(ContainerServiceElement.builder().build());
      rollbackElement = context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_ROLLBACK_REQUEST_PARAM);

      subscriptionId = infrastructureMapping instanceof AzureKubernetesInfrastructureMapping
          ? ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getSubscriptionId()
          : null;
      resourceGroup = infrastructureMapping instanceof AzureKubernetesInfrastructureMapping
          ? ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getResourceGroup()
          : null;
      Preconditions.checkState(isNotBlank(containerServiceDeploy.getInstanceCount()));
      instanceCount = Integer.valueOf(context.renderExpression(containerServiceDeploy.getInstanceCount()));

      downsizeInstanceCount = isNotBlank(containerServiceDeploy.getDownsizeInstanceCount())
          ? Integer.valueOf(context.renderExpression(containerServiceDeploy.getDownsizeInstanceCount()))
          : null;
    }
  }
}
