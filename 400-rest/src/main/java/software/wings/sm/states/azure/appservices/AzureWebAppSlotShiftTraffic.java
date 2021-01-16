package software.wings.sm.states.azure.appservices;

import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AZURE_APP_SERVICE_SLOT_TRAFFIC_SHIFT;
import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SHIFT_TRAFFIC;

import io.harness.azure.model.AzureConstants;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotShiftTrafficParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotShiftTrafficResponse;

import software.wings.beans.Activity;
import software.wings.beans.command.AzureWebAppCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
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
    azureSweepingOutputServiceHelper.saveTrafficShiftInfoToSweepingOutput(context, renderTrafficWeight(context));
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
    AzureAppServiceSlotSetupContextElement contextElement = readContextElement(context);
    return AzureAppServiceSlotShiftTrafficExecutionData.builder()
        .activityId(activity.getUuid())
        .infrastructureMappingId(azureAppServiceStateData.getInfrastructureMapping().getUuid())
        .appServiceName(contextElement.getWebApp())
        .deploySlotName(contextElement.getDeploymentSlot())
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
  protected List<CommandUnit> commandUnits() {
    return ImmutableList.of(new AzureWebAppCommandUnit(AzureConstants.SLOT_TRAFFIC_WEIGHT),
        new AzureWebAppCommandUnit(AzureConstants.DEPLOYMENT_STATUS));
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

  private AzureWebAppSlotShiftTrafficParameters buildTrafficShiftParams(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureAppServiceSlotSetupContextElement contextElement = readContextElement(context);

    return AzureWebAppSlotShiftTrafficParameters.builder()
        .accountId(azureAppServiceStateData.getApplication().getAccountId())
        .appId(azureAppServiceStateData.getApplication().getAppId())
        .activityId(activity.getUuid())
        .commandName(APP_SERVICE_SLOT_TRAFFIC_SHIFT)
        .timeoutIntervalInMin(contextElement.getAppServiceSlotSetupTimeOut())
        .subscriptionId(azureAppServiceStateData.getSubscriptionId())
        .resourceGroupName(azureAppServiceStateData.getResourceGroup())
        .webAppName(contextElement.getWebApp())
        .deploymentSlot(contextElement.getDeploymentSlot())
        .trafficWeightInPercentage(renderTrafficWeight(context))
        .preDeploymentData(contextElement.getPreDeploymentData())
        .build();
  }

  private float renderTrafficWeight(ExecutionContext context) {
    return azureVMSSStateHelper.renderFloatExpression(trafficWeightExpr, context, 0);
  }
}
