package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.DEPLOYMENT_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.azure.delegate.AzureAutoScaleSettingsHelperServiceDelegate;
import software.wings.service.intfc.azure.delegate.AzureVMSSHelperServiceDelegate;

@Slf4j
public abstract class AzureVMSSTaskHandler {
  @Inject protected AzureVMSSHelperServiceDelegate azureVMSSHelperServiceDelegate;
  @Inject protected AzureAutoScaleSettingsHelperServiceDelegate azureAutoScaleSettingsHelperServiceDelegate;
  @Inject protected DelegateLogService delegateLogService;
  @Inject protected TimeLimiter timeLimiter;

  public AzureVMSSTaskExecutionResponse executeTask(
      AzureVMSSTaskParameters azureVMSSTaskParameters, AzureConfig azureConfig) {
    try {
      AzureVMSSTaskExecutionResponse response = executeTaskInternal(azureVMSSTaskParameters, azureConfig);
      if (!azureVMSSTaskParameters.isSyncTask()) {
        ExecutionLogCallback logCallback = getLogCallBack(azureVMSSTaskParameters, DEPLOYMENT_ERROR);
        logCallback.saveExecutionLog("No deployment error. Execution success", INFO, SUCCESS);
      }
      return response;
    } catch (Exception ex) {
      if (azureVMSSTaskParameters.isSyncTask()) {
        throw new InvalidRequestException(ex.getMessage(), ex);
      } else {
        String message = ex.getMessage();
        ExecutionLogCallback logCallback = getLogCallBack(azureVMSSTaskParameters, DEPLOYMENT_ERROR);
        logCallback.saveExecutionLog(message, ERROR, FAILURE);
        logger.error(format("Exception: [%s] while processing azure vmss task: [%s].", message,
                         azureVMSSTaskParameters.getCommandType().name()),
            ex);
        return AzureVMSSTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
      }
    }
  }

  protected ExecutionLogCallback getLogCallBack(AzureVMSSTaskParameters parameters, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, parameters.getAccountId(), parameters.getAppId(), parameters.getActivityId(), commandUnit);
  }

  protected abstract AzureVMSSTaskExecutionResponse executeTaskInternal(
      AzureVMSSTaskParameters azureVMSSTaskParameters, AzureConfig azureConfig);
}
