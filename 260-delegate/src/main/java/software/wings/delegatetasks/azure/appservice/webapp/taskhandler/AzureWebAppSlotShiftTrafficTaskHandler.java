package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.azure.model.AzureConstants.SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotShiftTrafficParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotShiftTrafficResponse;
import io.harness.exception.InvalidArgumentsException;

import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

public class AzureWebAppSlotShiftTrafficTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppSlotShiftTrafficParameters slotShiftTrafficParameters =
        (AzureWebAppSlotShiftTrafficParameters) azureAppServiceTaskParameters;
    validateSlotShiftTrafficParameters(slotShiftTrafficParameters);

    AzureWebClientContext webClientContext = buildAzureWebClientContext(slotShiftTrafficParameters, azureConfig);

    updateSlotTrafficWeight(slotShiftTrafficParameters, webClientContext, logStreamingTaskClient);

    return AzureWebAppSlotShiftTrafficResponse.builder().build();
  }

  private void validateSlotShiftTrafficParameters(AzureWebAppSlotShiftTrafficParameters slotShiftTrafficParameters) {
    String webAppName = slotShiftTrafficParameters.getAppName();
    if (isBlank(webAppName)) {
      throw new InvalidArgumentsException(WEB_APP_NAME_BLANK_ERROR_MSG);
    }

    String shiftTrafficSlotName = slotShiftTrafficParameters.getDeploymentSlot();
    if (isBlank(shiftTrafficSlotName)) {
      throw new InvalidArgumentsException(SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG);
    }

    double trafficWeightInPercentage = slotShiftTrafficParameters.getTrafficWeightInPercentage();
    if (trafficWeightInPercentage > 100.0 || trafficWeightInPercentage < 0) {
      throw new InvalidArgumentsException(TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG);
    }
  }

  private void updateSlotTrafficWeight(AzureWebAppSlotShiftTrafficParameters slotShiftTrafficParameters,
      AzureWebClientContext webClientContext, ILogStreamingTaskClient logStreamingTaskClient) {
    String shiftTrafficSlotName = slotShiftTrafficParameters.getDeploymentSlot();
    double trafficWeightInPercentage = slotShiftTrafficParameters.getTrafficWeightInPercentage();

    azureAppServiceDeploymentService.rerouteProductionSlotTraffic(
        webClientContext, shiftTrafficSlotName, trafficWeightInPercentage, logStreamingTaskClient);
  }
}
