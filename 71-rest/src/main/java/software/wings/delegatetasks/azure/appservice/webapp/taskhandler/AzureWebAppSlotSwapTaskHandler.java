package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.azure.model.AzureConstants.SLOT_STOPPING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_SWAP;
import static io.harness.azure.model.AzureConstants.SOURCE_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.TARGET_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSwapSlotsParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSwapSlotsResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureTimeLimiter;
import software.wings.delegatetasks.azure.DefaultCompletableSubscriber;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import com.google.inject.Inject;
import com.microsoft.azure.management.monitor.EventData;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.joda.time.DateTime;

public class AzureWebAppSlotSwapTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Inject private AzureWebClient azureWebClient;
  @Inject private AzureMonitorClient azureMonitorClient;
  @Inject private AzureTimeLimiter azureTimeLimiter;

  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppSwapSlotsParameters slotSwapParameters = (AzureWebAppSwapSlotsParameters) azureAppServiceTaskParameters;
    validateSlotSwapParameters(slotSwapParameters);

    AzureWebClientContext webClientContext = buildAzureWebClientContext(slotSwapParameters, azureConfig);

    swapSlots(slotSwapParameters, webClientContext, logStreamingTaskClient);

    return AzureWebAppSwapSlotsResponse.builder().build();
  }

  private void validateSlotSwapParameters(AzureWebAppSwapSlotsParameters slotSwapParameters) {
    String webAppName = slotSwapParameters.getAppName();
    if (isBlank(webAppName)) {
      throw new InvalidArgumentsException(WEB_APP_NAME_BLANK_ERROR_MSG);
    }

    String sourceSlotName = slotSwapParameters.getSourceSlotName();
    if (isBlank(sourceSlotName)) {
      throw new InvalidArgumentsException(SOURCE_SLOT_NAME_BLANK_ERROR_MSG);
    }

    String targetSlotName = slotSwapParameters.getTargetSlotName();
    if (isBlank(targetSlotName)) {
      throw new InvalidArgumentsException(TARGET_SLOT_NAME_BLANK_ERROR_MSG);
    }
  }

  private void swapSlots(AzureWebAppSwapSlotsParameters slotSwapParameters, AzureWebClientContext webClientContext,
      ILogStreamingTaskClient logStreamingTaskClient) {
    LogCallback slotSwapLogCallback = logStreamingTaskClient.obtainLogCallback(SLOT_SWAP);
    Integer steadyStateTimeoutInMinutes = slotSwapParameters.getTimeoutIntervalInMin();
    String sourceSlotName = slotSwapParameters.getSourceSlotName();
    String targetSlotName = slotSwapParameters.getTargetSlotName();
    AzureConfig azureConfig = webClientContext.getAzureConfig();
    String subscriptionId = webClientContext.getSubscriptionId();
    String resourceGroupName = webClientContext.getResourceGroupName();
    DefaultCompletableSubscriber defaultSubscriber = new DefaultCompletableSubscriber();

    slotSwapLogCallback.saveExecutionLog(
        format("Sending request for swapping source [%s] slot with target [%s]", sourceSlotName, targetSlotName));
    AtomicReference<DateTime> startTime = new AtomicReference<>(DateTime.now());
    azureWebClient.swapDeploymentSlotsAsync(webClientContext, sourceSlotName, targetSlotName)
        .subscribe(defaultSubscriber);
    slotSwapLogCallback.saveExecutionLog("Request sent successfully");

    slotSwapLogCallback.saveExecutionLog(
        format("Swapping deployment source slot [%s] with target [%s]", sourceSlotName, targetSlotName));
    Supplier<Void> getSwappingStatus = () -> {
      slotSwapLogCallback.saveExecutionLog("Checking swapping slots status");
      List<EventData> eventData = azureMonitorClient.listEventDataWithAllPropertiesByResourceGroupName(
          azureConfig, subscriptionId, resourceGroupName, startTime.get(), DateTime.now());
      slotSwapLogCallback.saveExecutionLog(AzureResourceUtility.activityLogEventDataToString(eventData));
      startTime.set(DateTime.now());
      return null;
    };

    azureTimeLimiter.waitUntilCompleteWithTimeout(steadyStateTimeoutInMinutes, SLOT_STOPPING_STATUS_CHECK_INTERVAL,
        defaultSubscriber, getSwappingStatus, slotSwapLogCallback, SLOT_SWAP);
    slotSwapLogCallback.saveExecutionLog("Swapping slots done successfully", INFO, SUCCESS);
  }
}
