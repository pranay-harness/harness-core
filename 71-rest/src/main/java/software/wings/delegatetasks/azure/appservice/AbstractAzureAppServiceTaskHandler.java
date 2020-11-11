package software.wings.delegatetasks.azure.appservice;

import static io.harness.azure.model.AzureConstants.DEPLOYMENT_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

@Slf4j
public abstract class AbstractAzureAppServiceTaskHandler {
  @Inject protected DelegateLogService delegateLogService;

  AzureTaskExecutionResponse executeTask(
      AzureAppServiceTaskParameters azureAppServiceTaskParameters, AzureConfig azureConfig) {
    try {
      AzureAppServiceTaskResponse azureAppServiceTaskResponse =
          executeTaskInternal(azureAppServiceTaskParameters, azureConfig);
      return successAppServiceTaskResponse(azureAppServiceTaskResponse);
    } catch (Exception ex) {
      String message = AzureResourceUtility.getAzureCloudExceptionMessage(ex);
      if (azureAppServiceTaskParameters.isSyncTask()) {
        throw new InvalidRequestException(message, ex);
      }

      ExecutionLogCallback logCallback = getLogCallBack(azureAppServiceTaskParameters, DEPLOYMENT_ERROR);
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

  protected ExecutionLogCallback getLogCallBack(
      AzureAppServiceTaskParameters azureAppServiceTaskParameters, String commandUnit) {
    return new ExecutionLogCallback(delegateLogService, azureAppServiceTaskParameters.getAccountId(),
        azureAppServiceTaskParameters.getAppId(), azureAppServiceTaskParameters.getActivityId(), commandUnit);
  }

  private AzureTaskExecutionResponse failureAppServiceTaskResponse(String message) {
    return AzureTaskExecutionResponse.builder()
        .errorMessage(message)
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .build();
  }

  protected abstract AzureAppServiceTaskResponse executeTaskInternal(
      AzureAppServiceTaskParameters azureAppServiceTaskParameters, AzureConfig azureConfig);
}
