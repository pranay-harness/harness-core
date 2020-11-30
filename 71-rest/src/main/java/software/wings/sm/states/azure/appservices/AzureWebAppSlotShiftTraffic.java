package software.wings.sm.states.azure.appservices;

import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AZURE_APP_SERVICE_SLOT_TRAFFIC_SHIFT;
import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SHIFT_TRAFFIC;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotShiftTrafficParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotShiftTrafficResponse;

import software.wings.beans.Activity;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateExecutionData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotShiftTraffic extends AbstractAzureAppServiceState {
  @Getter @Setter private String trafficWeightExpr;
  public static final String APP_SERVICE_SLOT_TRAFFIC_SHIFT = "App Service Slot Traffic Shift";

  public AzureWebAppSlotShiftTraffic(String name) {
    super(name, AZURE_WEBAPP_SLOT_SHIFT_TRAFFIC);
  }

  @Override
  protected void emitAnyDataForExternalConsumption(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse) {
    azureSweepingOutputServiceHelper.saveInstanceInfoToSweepingOutput(context, renderTrafficWeight(context));
  }

  @Override
  protected AzureTaskExecutionRequest buildTaskExecutionRequest(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureWebAppSlotShiftTrafficParameters trafficShiftParams =
        buildTrafficShiftParams(context, azureAppServiceStateData, activity);

    return AzureTaskExecutionRequest.builder()
        .azureConfigDTO(azureVMSSStateHelper.createAzureConfigDTO(azureAppServiceStateData.getAzureConfig()))
        .azureConfigEncryptionDetails(azureAppServiceStateData.getAzureEncryptedDataDetails())
        .azureTaskParameters(trafficShiftParams)
        .build();
  }

  @Override
  protected StateExecutionData buildPreStateExecutionData(
      Activity activity, ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData) {
    return AzureAppServiceSlotShiftTrafficExecutionData.builder()
        .activityId(activity.getUuid())
        .infrastructureMappingId(azureAppServiceStateData.getInfrastructureMapping().getUuid())
        .appServiceName(azureAppServiceStateData.getAppService())
        .deploySlotName(azureAppServiceStateData.getDeploymentSlot())
        .trafficWeight(String.valueOf(renderTrafficWeight(context)))
        .appServiceSlotSetupTimeOut(getTimeoutMillis(context))
        .build();
  }

  @Override
  protected StateExecutionData buildPostStateExecutionData(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse, ExecutionStatus executionStatus) {
    AzureWebAppSlotShiftTrafficResponse slotSetupTaskResponse =
        (AzureWebAppSlotShiftTrafficResponse) executionResponse.getAzureTaskResponse();

    AzureAppServiceSlotShiftTrafficExecutionData stateExecutionData = context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    stateExecutionData.setAppServiceName(slotSetupTaskResponse.getPreDeploymentData().getAppName());
    stateExecutionData.setDeploySlotName(slotSetupTaskResponse.getPreDeploymentData().getSlotName());
    return stateExecutionData;
  }

  @Override
  protected ContextElement buildContextElement(ExecutionContext context, AzureTaskExecutionResponse executionResponse) {
    AzureWebAppSlotShiftTrafficResponse slotTrafficShiftResponse =
        (AzureWebAppSlotShiftTrafficResponse) executionResponse.getAzureTaskResponse();
    AzureAppServiceSlotShiftTrafficExecutionData stateExecutionData = context.getStateExecutionData();
    AzureAppServicePreDeploymentData preDeploymentData = slotTrafficShiftResponse.getPreDeploymentData();

    return AzureAppServiceSlotSetupContextElement.builder()
        .infraMappingId(stateExecutionData.getInfrastructureMappingId())
        .appServiceSlotSetupTimeOut(getTimeoutMillis(context))
        .commandName(APP_SERVICE_SLOT_TRAFFIC_SHIFT)
        .webApp(preDeploymentData.getAppName())
        .deploymentSlot(preDeploymentData.getSlotName())
        .preDeploymentData(preDeploymentData)
        .build();
  }

  @Override
  protected List<CommandUnit> commandUnits() {
    return ImmutableList.of();
  }

  @NotNull
  @Override
  protected CommandUnitDetails.CommandUnitType commandUnitType() {
    return AZURE_APP_SERVICE_SLOT_TRAFFIC_SHIFT;
  }

  @Override
  protected String commandType() {
    return APP_SERVICE_SLOT_TRAFFIC_SHIFT;
  }

  @NotNull
  @Override
  protected String errorMessageTag() {
    return "Azure App Service traffic shift failed";
  }

  private AzureWebAppSlotShiftTrafficParameters buildTrafficShiftParams(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureAppServiceSlotSetupContextElement contextElement = getContextElement(context);

    return AzureWebAppSlotShiftTrafficParameters.builder()
        .accountId(azureAppServiceStateData.getApplication().getAccountId())
        .appId(azureAppServiceStateData.getApplication().getAppId())
        .activityId(activity.getUuid())
        .commandName(APP_SERVICE_SLOT_TRAFFIC_SHIFT)
        .timeoutIntervalInMin(contextElement.getAppServiceSlotSetupTimeOut())
        .subscriptionId(azureAppServiceStateData.getSubscriptionId())
        .resourceGroupName(azureAppServiceStateData.getResourceGroup())
        .webAppName(azureAppServiceStateData.getAppService())
        .deploymentSlot(azureAppServiceStateData.getDeploymentSlot())
        .trafficWeightInPercentage(renderTrafficWeight(context))
        .preDeploymentData(contextElement.getPreDeploymentData())
        .build();
  }

  private int renderTrafficWeight(ExecutionContext context) {
    return azureVMSSStateHelper.renderExpressionOrGetDefault(trafficWeightExpr, context, 0);
  }
}
