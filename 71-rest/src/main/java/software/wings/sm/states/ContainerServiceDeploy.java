package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
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
import software.wings.beans.command.ContainerResizeParams;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ContainerServiceDeploy extends State {
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient DelegateService delegateService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private transient ContainerDeploymentManagerHelper containerDeploymentHelper;
  @Inject @Transient protected transient FeatureFlagService featureFlagService;
  @Inject @Transient private transient AwsCommandHelper awsCommandHelper;

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
              .withNamespace(contextData.containerElement.getNamespace())
              .withActivityId(activity.getUuid());

      if (isRollback()) {
        logger.info("Executing rollback");

        // Deployment of a K8 V2 service with a V1 workflow is not allowed. So if we reach here there is nothing to
        // rollback and hence we fail with an appropriate error message and exception
        Service service = serviceResourceService.get(contextData.app.getUuid(), contextData.service.getUuid());
        DeploymentType deploymentType = serviceResourceService.getDeploymentType(infrastructureMapping, service, null);
        if (deploymentType == DeploymentType.KUBERNETES) {
          if (context.getOrchestrationWorkflowType() != null && context.getOrchestrationWorkflowType() == BASIC
              && service.isK8sV2()) {
            throw new InvalidRequestException(
                "Kubernetes V2 service is not allowed to be deployed for 'Basic' workflow type, "
                + "so nothing to rollback.");
          }
        }

        if (contextData.rollbackElement == null) {
          return ExecutionResponse.builder()
              .executionStatus(SKIPPED)
              .stateExecutionData(
                  aStateExecutionData().withErrorMsg("No context found for rollback. Skipping.").build())
              .build();
        }

        executionDataBuilder.withNewInstanceData(contextData.rollbackElement.getNewInstanceData());
        executionDataBuilder.withOldInstanceData(contextData.rollbackElement.getOldInstanceData());
      }

      ContainerResizeParams params = buildContainerResizeParams(context, contextData);
      DeploymentType deploymentType =
          serviceResourceService.getDeploymentType(infrastructureMapping, contextData.service, null);

      CommandStateExecutionData executionData = executionDataBuilder.build();
      executionData.setServiceCounts(params.getOriginalServiceCounts());

      CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                            .accountId(contextData.app.getAccountId())
                                                            .appId(contextData.app.getUuid())
                                                            .envId(contextData.env.getUuid())
                                                            .activityId(activity.getUuid())
                                                            .cloudProviderSetting(contextData.settingAttribute)
                                                            .cloudProviderCredentials(contextData.encryptedDataDetails)
                                                            .containerResizeParams(params)
                                                            .deploymentType(deploymentType.name())
                                                            .build();

      String waitId = UUID.randomUUID().toString();
      String delegateTaskId = delegateService.queueTask(
          DelegateTask.builder()
              .accountId(contextData.app.getAccountId())
              .appId(contextData.appId)
              .waitId(waitId)
              .tags(awsCommandHelper.getAwsConfigTagsFromContext(commandExecutionContext))
              .data(TaskData.builder()
                        .async(true)
                        .taskType(TaskType.COMMAND.name())
                        .parameters(new Object[] {contextData.command, commandExecutionContext})
                        .timeout(TimeUnit.HOURS.toMillis(1))
                        .build())
              .envId(contextData.env.getUuid())
              .infrastructureMappingId(contextData.infrastructureMappingId)
              .build());

      return ExecutionResponse.builder()
          .async(true)
          .correlationIds(singletonList(waitId))
          .stateExecutionData(executionData)
          .delegateTaskId(delegateTaskId)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
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

      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
      ServiceElement serviceElement = phaseElement.getServiceElement();
      String serviceId = phaseElement.getServiceElement().getUuid();
      String appId = context.getAppId();
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Preconditions.checkNotNull(workflowStandardParams);
      Preconditions.checkNotNull(workflowStandardParams.getEnv());
      String envId = workflowStandardParams.getEnv().getUuid();
      executionData.setNewInstanceStatusSummaries(
          buildInstanceStatusSummaries(appId, serviceId, envId, serviceElement, response));

      updateContainerElementAfterSuccessfulResize(context);
      return buildEndStateExecution(executionData, commandExecutionResult, ExecutionStatus.SUCCESS);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected void updateContainerElementAfterSuccessfulResize(ExecutionContext context) {
    // Do Nothing here, let subclasses override it if required
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
    InstanceElementListParam listParam = InstanceElementListParam.builder().instanceElements(instanceElements).build();

    if (executionResult != null) {
      ResizeCommandUnitExecutionData resizeExecutionData =
          (ResizeCommandUnitExecutionData) executionResult.getCommandExecutionData();
      if (resizeExecutionData != null) {
        executionData.setNewInstanceData(resizeExecutionData.getNewInstanceData());
        executionData.setOldInstanceData(resizeExecutionData.getOldInstanceData());
        executionData.setNamespace(resizeExecutionData.getNamespace());
      }
      executionData.setDelegateMetaInfo(executionResult.getDelegateMetaInfo());
    }

    return ExecutionResponse.builder()
        .stateExecutionData(executionData)
        .executionStatus(status)
        .contextElement(listParam)
        .notifyElement(listParam)
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
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
      serviceId = phaseElement.getServiceElement().getUuid();
      appId = context.getAppId();
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      app = workflowStandardParams.getApp();
      env = workflowStandardParams.getEnv();
      Preconditions.checkNotNull(env);
      service = containerServiceDeploy.serviceResourceService.getWithDetails(appId, serviceId);
      command = containerServiceDeploy.serviceResourceService
                    .getCommandByName(appId, serviceId, env.getUuid(), containerServiceDeploy.getCommandName())
                    .getCommand();
      InfrastructureMapping infrastructureMapping = containerServiceDeploy.infrastructureMappingService.get(
          workflowStandardParams.getAppId(), context.fetchInfraMappingId());
      infrastructureMappingId = infrastructureMapping.getUuid();
      settingAttribute =
          containerServiceDeploy.settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      encryptedDataDetails = containerServiceDeploy.secretManager.getEncryptionDetails(
          (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
      region = infrastructureMapping instanceof EcsInfrastructureMapping
          ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
          : "";
      containerElement = context.<ContainerServiceElement>getContextElementList(ContextElementType.CONTAINER_SERVICE)
                             .stream()
                             .filter(cse -> phaseElement.getDeploymentType().equals(cse.getDeploymentType().name()))
                             .filter(cse -> context.fetchInfraMappingId().equals(cse.getInfraMappingId()))
                             .findFirst()
                             .orElse(ContainerServiceElement.builder().build());
      rollbackElement = context.getContextElement(
          ContextElementType.PARAM, ContainerRollbackRequestElement.CONTAINER_ROLLBACK_REQUEST_PARAM);

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
