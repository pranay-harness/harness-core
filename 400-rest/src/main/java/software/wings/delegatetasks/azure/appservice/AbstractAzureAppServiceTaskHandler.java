package software.wings.delegatetasks.azure.appservice;

import static io.harness.azure.model.AzureConstants.DEPLOYMENT_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAzureAppServiceTaskHandler {
  AzureTaskExecutionResponse executeTask(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    try {
      AzureAppServiceTaskResponse azureAppServiceTaskResponse =
          executeTaskInternal(azureAppServiceTaskParameters, azureConfig, logStreamingTaskClient);
      return handleAppServiceTaskResponse(azureAppServiceTaskResponse);
    } catch (Exception ex) {
      String message = AzureResourceUtility.getAzureCloudExceptionMessage(ex);
      if (azureAppServiceTaskParameters.isSyncTask()) {
        throw new InvalidRequestException(message, ex);
      }
      logErrorMsg(azureAppServiceTaskParameters, logStreamingTaskClient, ex, message);
      return failureAppServiceTaskResponse(message);
    }
  }

  private AzureTaskExecutionResponse handleAppServiceTaskResponse(
      AzureAppServiceTaskResponse azureAppServiceTaskResponse) {
    if (azureAppServiceTaskResponse instanceof AzureWebAppSlotSetupResponse) {
      AzureWebAppSlotSetupResponse azureWebAppSlotSetupResponse =
          (AzureWebAppSlotSetupResponse) azureAppServiceTaskResponse;
      String errorMsg = azureWebAppSlotSetupResponse.getErrorMsg();
      return isNoneBlank(errorMsg) ? failureAppServiceTaskResponse(azureAppServiceTaskResponse, errorMsg)
                                   : successAppServiceTaskResponse(azureAppServiceTaskResponse);
    }

    return successAppServiceTaskResponse(azureAppServiceTaskResponse);
  }

  private AzureTaskExecutionResponse successAppServiceTaskResponse(
      AzureAppServiceTaskResponse azureAppServiceTaskResponse) {
    return AzureTaskExecutionResponse.builder()
        .azureTaskResponse(azureAppServiceTaskResponse)
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .build();
  }

  private AzureTaskExecutionResponse failureAppServiceTaskResponse(
      AzureAppServiceTaskResponse azureAppServiceTaskResponse, String message) {
    return AzureTaskExecutionResponse.builder()
        .errorMessage(message)
        .azureTaskResponse(azureAppServiceTaskResponse)
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .build();
  }

  private AzureTaskExecutionResponse failureAppServiceTaskResponse(String message) {
    return AzureTaskExecutionResponse.builder()
        .errorMessage(message)
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .build();
  }

  protected void logErrorMsg(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      ILogStreamingTaskClient logStreamingTaskClient, Exception ex, String message) {
    LogCallback logCallback = logStreamingTaskClient.obtainLogCallback(DEPLOYMENT_ERROR);
    logCallback.saveExecutionLog(message, ERROR, FAILURE);
    log.error(format("Exception: [%s] while processing azure app service task: [%s].", message,
                  azureAppServiceTaskParameters.getCommandType().name()),
        ex);
  }

  protected abstract AzureAppServiceTaskResponse executeTaskInternal(
      AzureAppServiceTaskParameters azureAppServiceTaskParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient);
}
