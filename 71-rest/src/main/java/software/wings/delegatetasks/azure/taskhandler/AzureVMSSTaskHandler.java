package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_ERROR;
import static io.harness.azure.model.AzureConstants.VM_PROVISIONING_SPECIALIZED_STATUS;
import static io.harness.azure.model.AzureConstants.VM_PROVISIONING_SUCCEEDED_STATUS;
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
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.VirtualMachineInstanceView;
import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import com.microsoft.azure.management.compute.VirtualMachineScaleSetVM;
import io.harness.azure.client.AzureAutoScaleSettingsClient;
import io.harness.azure.client.AzureComputeClient;
import io.harness.azure.client.AzureNetworkClient;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AzureVMSSTaskHandler {
  @Inject protected AzureComputeClient azureComputeClient;
  @Inject protected AzureAutoScaleSettingsClient azureAutoScaleSettingsClient;
  @Inject protected AzureNetworkClient azureNetworkClient;
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
    ExecutionLogCallback logCallback = getLogCallBack(taskParameters, commandUnit);
    logCallback.saveExecutionLog(message, INFO, SUCCESS);
  }

  public ExecutionLogCallback getLogCallBack(AzureVMSSTaskParameters parameters, String commandUnit) {
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
        format("Set VMSS: [%s] desired capacity to [%s]", virtualMachineScaleSetName, capacity), INFO);

    VirtualMachineScaleSet vmss =
        getVirtualMachineScaleSet(azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);
    vmss.update().withCapacity(capacity).applyAsync().subscribe();

    logCallBack.saveExecutionLog("Successfully set desired capacity", INFO, SUCCESS);

    logCallBack = getLogCallBack(parameters, waitCommandUnit);
    waitForVMSSToBeDownSized(vmss, capacity, autoScalingSteadyStateTimeout, logCallBack);
    String message =
        "All the VM instances of VMSS: [%s] are " + (capacity == 0 ? "deleted " : "provisioned ") + "successfully";
    logCallBack.saveExecutionLog(format(message, virtualMachineScaleSetName), INFO);
  }

  private VirtualMachineScaleSet getVirtualMachineScaleSet(
      AzureConfig azureConfig, String subscriptionId, String resourceGroupName, String virtualMachineScaleSetName) {
    Optional<VirtualMachineScaleSet> updatedVMSSOp = azureComputeClient.getVirtualMachineScaleSetByName(
        azureConfig, subscriptionId, resourceGroupName, virtualMachineScaleSetName);
    return updatedVMSSOp.orElseThrow(
        ()
            -> new InvalidRequestException(
                format("There is no Virtual Machine Scale Set with name: %s, subscriptionId: %s, resourceGroupName: %s",
                    virtualMachineScaleSetName, subscriptionId, resourceGroupName)));
  }

  protected void waitForVMSSToBeDownSized(VirtualMachineScaleSet virtualMachineScaleSet, int capacity,
      int autoScalingSteadyStateTimeout, ExecutionLogCallback logCallBack) {
    try {
      timeLimiter.callWithTimeout(() -> {
        logCallBack.saveExecutionLog(
            format("Checking the status of: [%s] VM instances", virtualMachineScaleSet.name()), INFO);
        while (true) {
          if (checkAllVMSSInstancesProvisioned(virtualMachineScaleSet, capacity, logCallBack)) {
            return Boolean.TRUE;
          }
          sleep(ofSeconds(AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL));
        }
      }, autoScalingSteadyStateTimeout, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String message = "Timed out waiting for provisioning VMSS VM instances to desired capacity";
      logCallBack.saveExecutionLog(message, ERROR, FAILURE);
      throw new InvalidRequestException(message, e);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String message = "Error while waiting for provisioning VMSS VM instances to desired capacity";
      logCallBack.saveExecutionLog(message, ERROR, FAILURE);
      throw new InvalidRequestException(message, e);
    }
  }

  private boolean checkAllVMSSInstancesProvisioned(
      VirtualMachineScaleSet newVirtualMachineScaleSet, int desiredCapacity, ExecutionLogCallback logCallBack) {
    PagedList<VirtualMachineScaleSetVM> vmssInstanceList = newVirtualMachineScaleSet.virtualMachines().list();
    logVMInstancesStatus(vmssInstanceList, logCallBack);
    return desiredCapacity == 0 ? vmssInstanceList.isEmpty()
                                : desiredCapacity == vmssInstanceList.size()
            && vmssInstanceList.stream().allMatch(this ::isVMInstanceProvisioned);
  }

  private void logVMInstancesStatus(
      PagedList<VirtualMachineScaleSetVM> vmssInstanceList, ExecutionLogCallback logCallBack) {
    for (VirtualMachineScaleSetVM instance : vmssInstanceList) {
      String virtualMachineScaleSetVMName = instance.name();
      String provisioningDisplayStatus = getProvisioningDisplayStatus(instance);
      logCallBack.saveExecutionLog(String.format("Virtual machine instance: [%s] provisioning state: [%s]",
          virtualMachineScaleSetVMName, provisioningDisplayStatus));
    }
  }

  private boolean isVMInstanceProvisioned(VirtualMachineScaleSetVM instance) {
    String provisioningDisplayStatus = getProvisioningDisplayStatus(instance);
    return provisioningDisplayStatus.equals(VM_PROVISIONING_SPECIALIZED_STATUS)
        || provisioningDisplayStatus.equals(VM_PROVISIONING_SUCCEEDED_STATUS);
  }

  @NotNull
  private String getProvisioningDisplayStatus(VirtualMachineScaleSetVM instance) {
    return Optional.ofNullable(instance)
        .map(VirtualMachineScaleSetVM::instanceView)
        .map(VirtualMachineInstanceView::statuses)
        .map(instanceViewStatuses -> instanceViewStatuses.get(0))
        .map(InstanceViewStatus::displayStatus)
        .orElse(StringUtils.EMPTY);
  }
}
