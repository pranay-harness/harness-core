package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.azure.delegate.AzureAutoScaleSettingsHelperServiceDelegate;
import software.wings.service.intfc.azure.delegate.AzureNetworkHelperServiceDelegate;
import software.wings.service.intfc.azure.delegate.AzureVMSSHelperServiceDelegate;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AzureVMSSTaskHandler {
  @Inject protected AzureVMSSHelperServiceDelegate azureVMSSHelperServiceDelegate;
  @Inject protected AzureAutoScaleSettingsHelperServiceDelegate azureAutoScaleSettingsHelperServiceDelegate;
  @Inject protected AzureNetworkHelperServiceDelegate azureNetworkHelperServiceDelegate;
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
      String message = getErrorMessage(ex);
      if (azureVMSSTaskParameters.isSyncTask()) {
        throw new InvalidRequestException(message, ex);
      } else {
        ExecutionLogCallback logCallback = getLogCallBack(azureVMSSTaskParameters, DEPLOYMENT_ERROR);
        logCallback.saveExecutionLog(message, ERROR, FAILURE);
        logger.error(format("Exception: [%s] while processing azure vmss task: [%s].", message,
                         azureVMSSTaskParameters.getCommandType().name()),
            ex);
        return AzureVMSSTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
      }
    }
  }

  public String getErrorMessage(Exception ex) {
    String message = ex.getMessage();
    if (ex.getCause() instanceof CloudException) {
      CloudException cloudException = (CloudException) ex.getCause();
      String cloudExMsg = cloudException.getMessage();
      message = format("%s, %nAzure Cloud Exception Message: %s", message, cloudExMsg);
    }
    return message;
  }

  protected void createAndFinishEmptyExecutionLog(
      AzureVMSSTaskParameters taskParameters, String commandUnit, String message) {
    ExecutionLogCallback logCallback;
    logCallback = getLogCallBack(taskParameters, commandUnit);
    logCallback.saveExecutionLog(message, INFO, SUCCESS);
  }

  protected ExecutionLogCallback getLogCallBack(AzureVMSSTaskParameters parameters, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, parameters.getAccountId(), parameters.getAppId(), parameters.getActivityId(), commandUnit);
  }

  protected abstract AzureVMSSTaskExecutionResponse executeTaskInternal(
      AzureVMSSTaskParameters azureVMSSTaskParameters, AzureConfig azureConfig);

  protected void updateVMSSCapacityAndWaitForSteadyState(AzureConfig azureConfig, AzureVMSSTaskParameters parameters,
      String virtualMachineScaleSetName, String subscriptionId, String resourceGroupName, int capacity,
      int autoScalingSteadyStateTimeout, String scaleCommandUnit, String waitCommandUnit) {
    ExecutionLogCallback logCallBack = getLogCallBack(parameters, scaleCommandUnit);
    logCallBack.saveExecutionLog(
        format("Set VMSS : [%s] desired capacity to [%s]", virtualMachineScaleSetName, 0), INFO);
    VirtualMachineScaleSet updatedVMSS = azureVMSSHelperServiceDelegate.updateVMSSCapacity(
        azureConfig, virtualMachineScaleSetName, subscriptionId, resourceGroupName, capacity);
    logCallBack.saveExecutionLog("Successfully set desired capacity", INFO, SUCCESS);

    logCallBack = getLogCallBack(parameters, waitCommandUnit);
    waitForVMSSToBeDownSized(
        azureConfig, subscriptionId, updatedVMSS, capacity, autoScalingSteadyStateTimeout, logCallBack);
    logCallBack.saveExecutionLog("All instances are healthy", INFO, SUCCESS);
  }

  protected void waitForVMSSToBeDownSized(AzureConfig azureConfig, String subscriptionId,
      VirtualMachineScaleSet virtualMachineScaleSet, int capacity, int autoScalingSteadyStateTimeout,
      ExecutionLogCallback logCallBack) {
    try {
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          logCallBack.saveExecutionLog(
              format("Checking if [%d] VM instances of : [%s] are registered", capacity, virtualMachineScaleSet.name()),
              INFO);
          if (azureVMSSHelperServiceDelegate.checkIsRequiredNumberOfVMInstances(
                  azureConfig, subscriptionId, virtualMachineScaleSet.id(), capacity)) {
            return Boolean.TRUE;
          }
          sleep(ofSeconds(AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL));
        }
      }, autoScalingSteadyStateTimeout, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      logger.error("Timed out waiting for Virtual Machine Scale Set to be downsized to zero capacity", e);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(
          "Error while waiting for Virtual Machine Scale Set to be downsized to zero capacity", e);
    }
  }
}
