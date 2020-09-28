package software.wings.sm.states.azure;

import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_DESIRED_INSTANCES;
import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_MAX_INSTANCES;
import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_MIN_INSTANCES;
import static io.harness.azure.model.AzureConstants.DEFAULT_AZURE_VMSS_TIMEOUT_MIN;
import static io.harness.exception.ExceptionUtils.getMessage;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.sm.StateType.AZURE_VMSS_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.azure.model.AzureConstants;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.task.azure.request.AzureLoadBalancerDetailForBGDeployment;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSSetupTaskResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.service.impl.azure.manager.AzureVMSSCommandRequest;
import software.wings.service.impl.azure.manager.AzureVMSSCommandRequest.AzureVMSSCommandRequestBuilder;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.states.ManagerExecutionLogCallback;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureVMSSSetupState extends State {
  public static final String AZURE_VMSS_SETUP_COMMAND_NAME = AzureConstants.AZURE_VMSS_SETUP_COMMAND_NAME;

  @Getter @Setter private String virtualMachineScaleSetName;
  @Getter @Setter private String minInstances;
  @Getter @Setter private String maxInstances;
  @Getter @Setter private String desiredInstances;
  @Getter @Setter private String autoScalingSteadyStateVMSSTimeout;
  @Getter @Setter private boolean useCurrentRunningCount;
  @Getter @Setter private ResizeStrategy resizeStrategy;

  @Getter @Setter private AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail;

  @Inject private transient DelegateService delegateService;
  @Inject private transient AzureVMSSStateHelper azureVMSSStateHelper;

  public AzureVMSSSetupState(String name) {
    super(name, AZURE_VMSS_SETUP.name());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  @Override
  public Integer getTimeoutMillis(ExecutionContext context) {
    return azureVMSSStateHelper.getAzureVMSSStateTimeoutFromContext(context);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    Application app = azureVMSSStateHelper.getApplication(context);
    String appId = app.getUuid();

    Environment env = azureVMSSStateHelper.getEnvironment(context);
    String envId = env.getUuid();

    Service service = azureVMSSStateHelper.getServiceByAppId(context, appId);

    // create and save activity
    Activity activity =
        azureVMSSStateHelper.createAndSaveActivity(context, null, getStateType(), AZURE_VMSS_SETUP_COMMAND_NAME,
            CommandUnitDetails.CommandUnitType.AZURE_VMSS_SETUP, azureVMSSStateHelper.generateSetupCommandUnits());
    String activityId = activity.getUuid();

    ManagerExecutionLogCallback executionLogCallback = azureVMSSStateHelper.getExecutionLogCallback(activity);

    AzureVMSSInfrastructureMapping azureVMSSInfrastructureMapping =
        azureVMSSStateHelper.getAzureVMSSInfrastructureMapping(context.fetchInfraMappingId(), appId);

    int autoScalingSteadyStateVMSSTimeoutFixed = azureVMSSStateHelper.renderTimeoutExpressionOrGetDefault(
        autoScalingSteadyStateVMSSTimeout, context, DEFAULT_AZURE_VMSS_TIMEOUT_MIN);

    AzureVMSSTaskParameters azureVmssTaskParameters =
        buildAzureVMSSTaskParameters(context, app, service, env, activityId, azureVMSSInfrastructureMapping);

    AzureVMSSCommandRequest commandRequest =
        buildAzureVMSSCommandRequest(context, azureVMSSInfrastructureMapping, azureVmssTaskParameters);

    AzureVMSSSetupStateExecutionData azureVMSSSetupStateExecutionData =
        buildAzureVMSSSetupStateExecutionData(context, activityId, azureVMSSInfrastructureMapping.getUuid());

    executionLogCallback.saveExecutionLog("Starting Azure VMSS Setup");
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(app.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, appId)
            .waitId(activityId)
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.AZURE_VMSS_COMMAND_TASK.name())
                      .parameters(new Object[] {commandRequest})
                      .timeout(MINUTES.toMillis(autoScalingSteadyStateVMSSTimeoutFixed))
                      .build())
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, envId)
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, azureVMSSInfrastructureMapping.getUuid())
            .build();

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(singletonList(activityId))
        .stateExecutionData(azureVMSSSetupStateExecutionData)
        .async(true)
        .build();
  }

  private AzureVMSSTaskParameters buildAzureVMSSTaskParameters(ExecutionContext context, Application app,
      Service service, Environment env, String activityId,
      AzureVMSSInfrastructureMapping azureVMSSInfrastructureMapping) {
    Artifact artifact = azureVMSSStateHelper.getArtifact((DeploymentExecutionContext) context, service.getUuid());

    String baseVMSSName = azureVMSSInfrastructureMapping.getBaseVMSSName();
    String subscriptionId = azureVMSSInfrastructureMapping.getSubscriptionId();
    String resourceGroupName = azureVMSSInfrastructureMapping.getResourceGroupName();
    String vmssDeploymentType = azureVMSSInfrastructureMapping.getVmssDeploymentType().name();
    String infraMappingId = azureVMSSInfrastructureMapping.getUuid();

    String accountId = app.getAccountId();
    String appId = app.getAppId();
    boolean isBlueGreen = azureVMSSStateHelper.isBlueGreenWorkflow(context);
    String artifactRevision = artifact.getRevision();
    String envId = env.getUuid();
    String userData = azureVMSSStateHelper.getBase64EncodedUserData(context, app.getUuid(), service.getUuid());

    String vmssNamePrefixFixed = azureVMSSStateHelper.fixNamePrefix(
        context, virtualMachineScaleSetName, app.getName(), service.getName(), env.getName());

    int autoScalingSteadyStateVMSSTimeoutFixed = azureVMSSStateHelper.renderTimeoutExpressionOrGetDefault(
        autoScalingSteadyStateVMSSTimeout, context, DEFAULT_AZURE_VMSS_TIMEOUT_MIN);

    int maxInstancesFixed =
        azureVMSSStateHelper.renderExpressionOrGetDefault(maxInstances, context, DEFAULT_AZURE_VMSS_MAX_INSTANCES);
    int minInstancesFixed =
        azureVMSSStateHelper.renderExpressionOrGetDefault(minInstances, context, DEFAULT_AZURE_VMSS_MIN_INSTANCES);
    int desiredInstancesFixed = azureVMSSStateHelper.renderExpressionOrGetDefault(
        desiredInstances, context, DEFAULT_AZURE_VMSS_DESIRED_INSTANCES);

    AzureVMAuthDTO azureVmAuthDTO = azureVMSSStateHelper.createVMAuthDTO(azureVMSSInfrastructureMapping);
    List<EncryptedDataDetail> vmAuthDTOEncryptionDetails =
        azureVMSSStateHelper.getVMAuthDTOEncryptionDetails(context, azureVmAuthDTO, envId);
    azureVMSSStateHelper.updateEncryptedDataDetailSecretFieldName(azureVmAuthDTO, vmAuthDTOEncryptionDetails);

    return AzureVMSSSetupTaskParameters.builder()
        .accountId(accountId)
        .appId(appId)
        .activityId(activityId)
        .commandName(AZURE_VMSS_SETUP_COMMAND_NAME)
        .blueGreen(isBlueGreen)
        .azureLoadBalancerDetail(azureLoadBalancerDetail)
        .vmssNamePrefix(vmssNamePrefixFixed)
        .artifactRevision(artifactRevision)
        .baseVMSSName(baseVMSSName)
        .subscriptionId(subscriptionId)
        .resourceGroupName(resourceGroupName)
        .userData(userData)
        .azureVmAuthDTO(azureVmAuthDTO)
        .vmAuthDTOEncryptionDetails(vmAuthDTOEncryptionDetails)
        .vmssDeploymentType(vmssDeploymentType)
        .infraMappingId(infraMappingId)
        .minInstances(minInstancesFixed)
        .maxInstances(maxInstancesFixed)
        .desiredInstances(desiredInstancesFixed)
        .autoScalingSteadyStateVMSSTimeout(autoScalingSteadyStateVMSSTimeoutFixed)
        .useCurrentRunningCount(useCurrentRunningCount)
        .build();
  }

  private AzureVMSSCommandRequest buildAzureVMSSCommandRequest(ExecutionContext context,
      AzureVMSSInfrastructureMapping azureVMSSInfrastructureMapping, AzureVMSSTaskParameters azureVmssTaskParameters) {
    // AzureConfig
    String computeProviderSettingId = azureVMSSInfrastructureMapping.getComputeProviderSettingId();
    AzureConfig azureConfig = azureVMSSStateHelper.getAzureConfig(computeProviderSettingId);
    List<EncryptedDataDetail> azureEncryptionDetails =
        azureVMSSStateHelper.getEncryptedDataDetails(context, computeProviderSettingId);

    AzureVMSSCommandRequestBuilder azureVMSSCommandRequestBuilder =
        AzureVMSSCommandRequest.builder()
            .azureConfigDTO(azureVMSSStateHelper.createAzureConfigDTO(azureConfig))
            .azureConfigEncryptionDetails(azureEncryptionDetails)
            .azureVMSSTaskParameters(azureVmssTaskParameters);

    return azureVMSSCommandRequestBuilder.build();
  }

  @NotNull
  private AzureVMSSSetupStateExecutionData buildAzureVMSSSetupStateExecutionData(
      ExecutionContext context, String activityId, String infrastructureMappingId) {
    int maxInstancesFixed =
        azureVMSSStateHelper.renderExpressionOrGetDefault(maxInstances, context, DEFAULT_AZURE_VMSS_MAX_INSTANCES);
    int desiredInstancesFixed = azureVMSSStateHelper.renderExpressionOrGetDefault(
        desiredInstances, context, DEFAULT_AZURE_VMSS_DESIRED_INSTANCES);

    return AzureVMSSSetupStateExecutionData.builder()
        .activityId(activityId)
        .infrastructureMappingId(infrastructureMappingId)
        .maxInstances(useCurrentRunningCount ? null : maxInstancesFixed)
        .desiredInstances(useCurrentRunningCount ? null : desiredInstancesFixed)
        .resizeStrategy(resizeStrategy)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    AzureVMSSTaskExecutionResponse executionResponse =
        (AzureVMSSTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = azureVMSSStateHelper.getExecutionStatus(executionResponse);
    if (executionStatus == ExecutionStatus.FAILED) {
      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .errorMessage(executionResponse.getErrorMessage())
          .build();
    }

    updateActivityStatus(response, context.getAppId(), executionStatus);

    AzureVMSSSetupStateExecutionData stateExecutionData =
        populateAzureVMSSSetupStateExecutionData(context, executionResponse, executionStatus);
    AzureVMSSSetupContextElement azureVMSSSetupContextElement =
        buildAzureVMSSSetupContextElement(context, executionResponse);

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .contextElement(azureVMSSSetupContextElement)
        .notifyElement(azureVMSSSetupContextElement)
        .build();
  }

  private void updateActivityStatus(Map<String, ResponseData> response, String appId, ExecutionStatus executionStatus) {
    if (response.keySet().iterator().hasNext()) {
      String activityId = response.keySet().iterator().next();
      azureVMSSStateHelper.updateActivityStatus(appId, activityId, executionStatus);
    }
  }

  private AzureVMSSSetupContextElement buildAzureVMSSSetupContextElement(
      ExecutionContext context, AzureVMSSTaskExecutionResponse executionResponse) {
    AzureVMSSSetupTaskResponse azureVMSSSetupTaskResponse =
        (AzureVMSSSetupTaskResponse) executionResponse.getAzureVMSSTaskResponse();
    AzureVMSSSetupStateExecutionData azureVMSSSetupStateExecutionData =
        (AzureVMSSSetupStateExecutionData) context.getStateExecutionData();

    boolean isBlueGreen = azureVMSSStateHelper.isBlueGreenWorkflow(context);
    ResizeStrategy resizeStrategyFixed = getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy();
    int autoScalingSteadyStateVMSSTimeoutFixed = azureVMSSStateHelper.renderTimeoutExpressionOrGetDefault(
        autoScalingSteadyStateVMSSTimeout, context, DEFAULT_AZURE_VMSS_TIMEOUT_MIN);
    String infrastructureMappingId = azureVMSSSetupStateExecutionData.getInfrastructureMappingId();

    return AzureVMSSSetupContextElement.builder()
        .isBlueGreen(isBlueGreen)
        .infraMappingId(infrastructureMappingId)
        .azureLoadBalancerDetail(azureLoadBalancerDetail)
        .resizeStrategy(resizeStrategyFixed)
        .autoScalingSteadyStateVMSSTimeout(autoScalingSteadyStateVMSSTimeoutFixed)
        .commandName(AZURE_VMSS_SETUP_COMMAND_NAME)
        .newVirtualMachineScaleSetName(azureVMSSSetupTaskResponse.getNewVirtualMachineScaleSetName())
        .oldVirtualMachineScaleSetName(azureVMSSSetupTaskResponse.getLastDeployedVMSSName())
        .baseVMSSScalingPolicyJSONs(azureVMSSSetupTaskResponse.getBaseVMSSScalingPolicyJSONs())
        .minInstances(azureVMSSSetupTaskResponse.getMinInstances())
        .maxInstances(azureVMSSSetupTaskResponse.getMaxInstances())
        .desiredInstances(azureVMSSSetupTaskResponse.getDesiredInstances())
        .preDeploymentData(azureVMSSSetupTaskResponse.getPreDeploymentData())
        .build();
  }

  @NotNull
  private AzureVMSSSetupStateExecutionData populateAzureVMSSSetupStateExecutionData(
      ExecutionContext context, AzureVMSSTaskExecutionResponse executionResponse, ExecutionStatus executionStatus) {
    AzureVMSSSetupTaskResponse azureVMSSSetupTaskResponse =
        (AzureVMSSSetupTaskResponse) executionResponse.getAzureVMSSTaskResponse();

    AzureVMSSSetupStateExecutionData stateExecutionData = context.getStateExecutionData();

    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());

    stateExecutionData.setNewVirtualMachineScaleSetName(azureVMSSSetupTaskResponse.getNewVirtualMachineScaleSetName());
    stateExecutionData.setOldVirtualMachineScaleSetName(azureVMSSSetupTaskResponse.getLastDeployedVMSSName());
    stateExecutionData.setNewVersion(azureVMSSSetupTaskResponse.getHarnessRevision());
    stateExecutionData.setDelegateMetaInfo(azureVMSSSetupTaskResponse.getDelegateMetaInfo());
    stateExecutionData.setDesiredInstances(azureVMSSSetupTaskResponse.getDesiredInstances());
    stateExecutionData.setMaxInstances(azureVMSSSetupTaskResponse.getMaxInstances());

    return stateExecutionData;
  }
}
