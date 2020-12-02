package software.wings.sm.states.azure.appservices;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AZURE_APP_SERVICE_SLOT_SWAP;
import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SWAP;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSwapSlotsParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSwapSlotsResponse;

import software.wings.beans.Activity;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateExecutionData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotSwap extends AbstractAzureAppServiceState {
  @Getter @Setter private String subscriptionId;
  @Getter @Setter private String resourceGroup;
  @Getter @Setter private String webApp;
  @Getter @Setter private String targetSlot;
  public static final String APP_SERVICE_SLOT_SWAP = "App Service Slot Swap";

  public AzureWebAppSlotSwap(String name) {
    super(name, AZURE_WEBAPP_SLOT_SWAP);
  }

  @Override
  protected AzureTaskExecutionRequest buildTaskExecutionRequest(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureWebAppSwapSlotsParameters swapSlotsParameters =
        buildSlotSwapParams(context, azureAppServiceStateData, activity);

    return AzureTaskExecutionRequest.builder()
        .azureConfigDTO(azureVMSSStateHelper.createAzureConfigDTO(azureAppServiceStateData.getAzureConfig()))
        .azureConfigEncryptionDetails(azureAppServiceStateData.getAzureEncryptedDataDetails())
        .azureTaskParameters(swapSlotsParameters)
        .build();
  }

  @Override
  protected StateExecutionData buildPreStateExecutionData(
      Activity activity, ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData) {
    return AzureAppServiceSlotSwapExecutionData.builder()
        .activityId(activity.getUuid())
        .infrastructureMappingId(azureAppServiceStateData.getInfrastructureMapping().getUuid())
        .resourceGroup(resourceGroup)
        .appServiceName(webApp)
        .deploymentSlot(azureAppServiceStateData.getDeploymentSlot())
        .targetSlot(targetSlot)
        .build();
  }

  @Override
  protected StateExecutionData buildPostStateExecutionData(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse, ExecutionStatus executionStatus) {
    AzureAppServiceSlotSwapExecutionData stateExecutionData = context.getStateExecutionData();
    AzureWebAppSwapSlotsResponse swapSlotsResponse =
        (AzureWebAppSwapSlotsResponse) executionResponse.getAzureTaskResponse();

    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    stateExecutionData.setAppServiceName(swapSlotsResponse.getPreDeploymentData().getAppName());
    stateExecutionData.setDeploymentSlot(swapSlotsResponse.getPreDeploymentData().getSlotName());
    return stateExecutionData;
  }

  @Override
  protected String commandType() {
    return APP_SERVICE_SLOT_SWAP;
  }

  @NotNull
  @Override
  protected CommandUnitType commandUnitType() {
    return AZURE_APP_SERVICE_SLOT_SWAP;
  }

  @Override
  protected List<CommandUnit> commandUnits() {
    return ImmutableList.of();
  }

  @NotNull
  @Override
  protected String errorMessageTag() {
    return "Azure App Service swap slot failed";
  }

  private AzureWebAppSwapSlotsParameters buildSlotSwapParams(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureAppServiceSlotSetupContextElement contextElement = readContextElement(context);

    return AzureWebAppSwapSlotsParameters.builder()
        .accountId(azureAppServiceStateData.getApplication().getAccountId())
        .appId(azureAppServiceStateData.getApplication().getAppId())
        .activityId(activity.getUuid())
        .commandName(APP_SERVICE_SLOT_SWAP)
        .timeoutIntervalInMin(contextElement.getAppServiceSlotSetupTimeOut())
        .subscriptionId(subscriptionId)
        .resourceGroupName(resourceGroup)
        .webAppName(webApp)
        .sourceSlotName(contextElement.getDeploymentSlot())
        .targetSlotName(targetSlot)
        .preDeploymentData(contextElement.getPreDeploymentData())
        .build();
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(subscriptionId)) {
      invalidFields.put("Subscription Id", "Subscription Id must be specified");
    }
    if (isEmpty(resourceGroup)) {
      invalidFields.put("Resource Group", "Resource Group name must be specified");
    }
    if (isEmpty(webApp)) {
      invalidFields.put("Web App", "Web App name must be specified");
    }
    if (isEmpty(targetSlot)) {
      invalidFields.put("Target Slot", "Target Slot name must be specified");
    }
    return invalidFields;
  }
}
