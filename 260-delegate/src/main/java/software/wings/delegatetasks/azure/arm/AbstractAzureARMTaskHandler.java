package software.wings.delegatetasks.azure.arm;

import static io.harness.azure.model.AzureConstants.DEPLOYMENT_STATUS;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;
import io.harness.delegate.task.azure.arm.response.AzureARMDeploymentResponse;
import io.harness.exception.AzureClientException;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public abstract class AbstractAzureARMTaskHandler {
  AzureTaskExecutionResponse executeTask(AzureARMTaskParameters azureARMTaskParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    try {
      AzureARMTaskResponse azureARMTaskResponse =
          executeTaskInternal(azureARMTaskParameters, azureConfig, logStreamingTaskClient);

      return handleARMTaskResponse(azureARMTaskResponse);
    } catch (AzureClientException ex) {
      throw ex;
    } catch (Exception ex) {
      String message = AzureResourceUtility.getAzureCloudExceptionMessage(ex);
      if (azureARMTaskParameters.isSyncTask()) {
        throw new InvalidRequestException(message, ex);
      }

      logErrorMsg(azureARMTaskParameters, logStreamingTaskClient, ex, message);
      return handleFailureARMTaskResponse(message);
    }
  }

  private AzureTaskExecutionResponse handleARMTaskResponse(AzureARMTaskResponse azureARMTaskResponse) {
    return azureARMTaskResponse.getErrorMsg() == null
        ? successARMTaskResponse(azureARMTaskResponse)
        : failureARMTaskResponse(azureARMTaskResponse, azureARMTaskResponse.getErrorMsg());
  }

  private AzureTaskExecutionResponse successARMTaskResponse(AzureARMTaskResponse azureARMTaskResponse) {
    return AzureTaskExecutionResponse.builder()
        .azureTaskResponse(azureARMTaskResponse)
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .build();
  }

  private AzureTaskExecutionResponse failureARMTaskResponse(AzureARMTaskResponse azureARMTaskResponse, String message) {
    return AzureTaskExecutionResponse.builder()
        .errorMessage(message)
        .azureTaskResponse(azureARMTaskResponse)
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .build();
  }

  private AzureTaskExecutionResponse handleFailureARMTaskResponse(String message) {
    return AzureTaskExecutionResponse.builder()
        .azureTaskResponse(AzureARMDeploymentResponse.builder().errorMsg(message).build())
        .errorMessage(message)
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .build();
  }

  protected void logErrorMsg(AzureARMTaskParameters azureARMTaskParameters,
      ILogStreamingTaskClient logStreamingTaskClient, Exception ex, String message) {
    LogCallback logCallback = logStreamingTaskClient.obtainLogCallback(DEPLOYMENT_STATUS);
    logCallback.saveExecutionLog(message, ERROR, FAILURE);
    log.error(format("Exception: [%s] while processing Azure ARM task: [%s].", message,
                  azureARMTaskParameters.getCommandType().name()),
        ex);
  }

  protected abstract AzureARMTaskResponse executeTaskInternal(AzureARMTaskParameters azureARMTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient);
}
