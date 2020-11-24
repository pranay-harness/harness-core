package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.azure.model.AzureConstants.SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SLOT_TRAFFIC_WEIGHT;
import static io.harness.azure.model.AzureConstants.TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotShiftTrafficParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotShiftTrafficResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import com.google.inject.Inject;

public class AzureWebAppSlotShiftTrafficTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Inject private AzureWebClient azureWebClient;

  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppSlotShiftTrafficParameters slotShiftTrafficParameters =
        (AzureWebAppSlotShiftTrafficParameters) azureAppServiceTaskParameters;
    validateSlotShiftTrafficParameters(slotShiftTrafficParameters);

    AzureWebClientContext webClientContext = buildAzureWebClientContext(slotShiftTrafficParameters, azureConfig);

    rerouteProductionSlotTraffic(slotShiftTrafficParameters, webClientContext, logStreamingTaskClient);

    return AzureWebAppSlotShiftTrafficResponse.builder().build();
  }

  private void validateSlotShiftTrafficParameters(AzureWebAppSlotShiftTrafficParameters slotShiftTrafficParameters) {
    String webAppName = slotShiftTrafficParameters.getAppName();
    if (isBlank(webAppName)) {
      throw new InvalidArgumentsException(WEB_APP_NAME_BLANK_ERROR_MSG);
    }

    String shiftTrafficSlotName = slotShiftTrafficParameters.getShiftTrafficSlotName();
    if (isBlank(shiftTrafficSlotName)) {
      throw new InvalidArgumentsException(SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG);
    }

    double trafficWeightInPercentage = slotShiftTrafficParameters.getTrafficWeightInPercentage();
    if (trafficWeightInPercentage > 100.0 || trafficWeightInPercentage < 0) {
      throw new InvalidArgumentsException(TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG);
    }
  }

  private void rerouteProductionSlotTraffic(AzureWebAppSlotShiftTrafficParameters slotShiftTrafficParameters,
      AzureWebClientContext webClientContext, ILogStreamingTaskClient logStreamingTaskClient) {
    String shiftTrafficSlotName = slotShiftTrafficParameters.getShiftTrafficSlotName();
    double trafficWeightInPercentage = slotShiftTrafficParameters.getTrafficWeightInPercentage();

    LogCallback rerouteTrafficLogCallback = logStreamingTaskClient.obtainLogCallback(SLOT_TRAFFIC_WEIGHT);

    rerouteTrafficLogCallback.saveExecutionLog(format(
        "Start rerouting [%.2f] traffic to deployment slot: [%s] ", trafficWeightInPercentage, shiftTrafficSlotName));
    azureWebClient.rerouteProductionSlotTraffic(webClientContext, shiftTrafficSlotName, trafficWeightInPercentage);
    rerouteTrafficLogCallback.saveExecutionLog("Traffic rerouted successfully", INFO, SUCCESS);
  }
}
