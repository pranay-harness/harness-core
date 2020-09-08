package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.azure.model.AzureConstants.DELETE_NEW_VMSS;
import static io.harness.azure.model.AzureConstants.REQUEST_DELETE_SCALE_SET;
import static io.harness.azure.model.AzureConstants.SUCCESS_DELETE_SCALE_SET;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static java.lang.String.format;

import com.google.inject.Singleton;

import com.microsoft.azure.management.compute.VirtualMachineScaleSet;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.task.azure.request.AzureVMSSDeployTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.ExceptionUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.Optional;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureVMSSRollbackTaskHandler extends AzureVMSSDeployTaskHandler {
  @Override
  protected AzureVMSSTaskExecutionResponse executeTaskInternal(
      final AzureVMSSTaskParameters azureVMSSTaskParameters, final AzureConfig azureConfig) {
    AzureVMSSDeployTaskParameters deployTaskParameters = (AzureVMSSDeployTaskParameters) azureVMSSTaskParameters;
    try {
      deployTaskParameters.setResizeNewFirst(false);
      AzureVMSSTaskExecutionResponse azureVMSSTaskExecutionResponse =
          super.executeTaskInternal(deployTaskParameters, azureConfig);
      if (isSuccess(azureVMSSTaskExecutionResponse)) {
        deleteNewScaleSetIfPresent(azureConfig, deployTaskParameters);
      }
      return azureVMSSTaskExecutionResponse;
    } catch (Exception ex) {
      return rollBackFailureResponse(deployTaskParameters, ex);
    }
  }

  private void deleteNewScaleSetIfPresent(AzureConfig azureConfig, AzureVMSSDeployTaskParameters deployTaskParameters) {
    Optional<VirtualMachineScaleSet> scaleSet =
        getScaleSet(azureConfig, deployTaskParameters, deployTaskParameters.getNewVirtualMachineScaleSetName());

    if (scaleSet.isPresent()) {
      VirtualMachineScaleSet virtualMachineScaleSet = scaleSet.get();
      ExecutionLogCallback logCallback = getLogCallBack(deployTaskParameters, DELETE_NEW_VMSS);
      logCallback.saveExecutionLog(format(REQUEST_DELETE_SCALE_SET, virtualMachineScaleSet.name()));
      azureComputeClient.deleteVirtualMachineScaleSetById(azureConfig, virtualMachineScaleSet.id());
      logCallback.saveExecutionLog(format(SUCCESS_DELETE_SCALE_SET, virtualMachineScaleSet.name()), INFO, SUCCESS);
    }
  }

  private boolean isSuccess(AzureVMSSTaskExecutionResponse azureVMSSTaskExecutionResponse) {
    return azureVMSSTaskExecutionResponse.getCommandExecutionStatus() == SUCCESS;
  }

  private AzureVMSSTaskExecutionResponse rollBackFailureResponse(
      AzureVMSSDeployTaskParameters deployTaskParameters, Exception ex) {
    ExecutionLogCallback logCallBack = getLogCallBack(deployTaskParameters, deployTaskParameters.getCommandName());
    String errorMessage = ExceptionUtils.getMessage(ex);
    logCallBack.saveExecutionLog(format("Exception: [%s].", errorMessage), ERROR);
    logger.error(errorMessage, ex);
    return AzureVMSSTaskExecutionResponse.builder().errorMessage(errorMessage).commandExecutionStatus(FAILURE).build();
  }
}
