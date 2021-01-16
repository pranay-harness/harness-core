package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.azure.model.AzureConstants.SOURCE_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.TARGET_SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSwapSlotsParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSwapSlotsResponse;
import io.harness.exception.InvalidArgumentsException;

import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

public class AzureWebAppSlotSwapTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppSwapSlotsParameters slotSwapParameters = (AzureWebAppSwapSlotsParameters) azureAppServiceTaskParameters;
    validateSlotSwapParameters(slotSwapParameters);

    AzureWebClientContext webClientContext = buildAzureWebClientContext(slotSwapParameters, azureConfig);

    swapSlots(slotSwapParameters, webClientContext, logStreamingTaskClient);

    markExecutionAsSuccess(azureAppServiceTaskParameters, logStreamingTaskClient);
    return AzureWebAppSwapSlotsResponse.builder().preDeploymentData(slotSwapParameters.getPreDeploymentData()).build();
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
    String targetSlotName = slotSwapParameters.getTargetSlotName();
    Integer timeoutIntervalInMin = slotSwapParameters.getTimeoutIntervalInMin();

    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext = toAzureAppServiceDeploymentContext(
        slotSwapParameters, webClientContext, timeoutIntervalInMin, logStreamingTaskClient);

    azureAppServiceDeploymentService.swapSlotsUsingCallback(
        azureAppServiceDeploymentContext, targetSlotName, logStreamingTaskClient);
  }

  private AzureAppServiceDeploymentContext toAzureAppServiceDeploymentContext(
      AzureWebAppSwapSlotsParameters slotSwapParameters, AzureWebClientContext azureWebClientContext,
      Integer steadyTimeoutIntervalInMin, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureAppServiceDeploymentContext azureAppServiceDeploymentContext = new AzureAppServiceDeploymentContext();
    azureAppServiceDeploymentContext.setAzureWebClientContext(azureWebClientContext);
    azureAppServiceDeploymentContext.setLogStreamingTaskClient(logStreamingTaskClient);
    azureAppServiceDeploymentContext.setSlotName(slotSwapParameters.getSourceSlotName());
    azureAppServiceDeploymentContext.setSteadyStateTimeoutInMin(steadyTimeoutIntervalInMin);
    return azureAppServiceDeploymentContext;
  }
}
