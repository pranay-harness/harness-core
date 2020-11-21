package software.wings.delegatetasks.azure.appservice;

import static io.harness.azure.model.AzureConstants.DEPLOYMENT_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;

import static java.lang.String.format;

import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.DelegateLogService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAzureAppServiceTaskHandler {
  @Inject protected DelegateLogService delegateLogService;

  AzureTaskExecutionResponse executeTask(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    try {
      AzureAppServiceTaskResponse azureAppServiceTaskResponse =
          executeTaskInternal(azureAppServiceTaskParameters, azureConfig, logStreamingTaskClient);
      return successAppServiceTaskResponse(azureAppServiceTaskResponse);
    } catch (Exception ex) {
      String message = AzureResourceUtility.getAzureCloudExceptionMessage(ex);
      if (azureAppServiceTaskParameters.isSyncTask()) {
        throw new InvalidRequestException(message, ex);
      }
      LogCallback logCallback = logStreamingTaskClient.obtainLogCallback(DEPLOYMENT_ERROR);
      logCallback.saveExecutionLog(message, ERROR, FAILURE);
      log.error(format("Exception: [%s] while processing azure app service task: [%s].", message,
                    azureAppServiceTaskParameters.getCommandType().name()),
          ex);
      return failureAppServiceTaskResponse(message);
    }
  }

  private AzureTaskExecutionResponse successAppServiceTaskResponse(
      AzureAppServiceTaskResponse azureAppServiceTaskResponse) {
    return AzureTaskExecutionResponse.builder()
        .azureTaskResponse(azureAppServiceTaskResponse)
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .build();
  }

  private AzureTaskExecutionResponse failureAppServiceTaskResponse(String message) {
    return AzureTaskExecutionResponse.builder()
        .errorMessage(message)
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .build();
  }

  protected abstract AzureAppServiceTaskResponse executeTaskInternal(
      AzureAppServiceTaskParameters azureAppServiceTaskParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient);
}
