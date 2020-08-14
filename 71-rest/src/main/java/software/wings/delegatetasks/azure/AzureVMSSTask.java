package software.wings.delegatetasks.azure;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSyncTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSTaskHandler;
import software.wings.service.impl.azure.manager.AzureVMSSCommandRequest;
import software.wings.service.intfc.security.EncryptionService;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class AzureVMSSTask extends AbstractDelegateRunnableTask {
  @Inject private AzureVMSSSyncTaskHandler azureVMSSSyncTaskHandler;
  @Inject private EncryptionService encryptionService;

  public AzureVMSSTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AzureVMSSTaskExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented.");
  }

  @Override
  public AzureVMSSTaskExecutionResponse run(TaskParameters parameters) {
    if (!(parameters instanceof AzureVMSSCommandRequest)) {
      String message =
          format("Unrecognized task params while running azure vmss task: [%s]", parameters.getClass().getSimpleName());
      logger.error(message);
      return AzureVMSSTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    AzureVMSSCommandRequest azureVMSSCommandRequest = (AzureVMSSCommandRequest) parameters;
    AzureVMSSTaskParameters azureVMSSTaskParameters = azureVMSSCommandRequest.getAzureVMSSTaskParameters();

    if (azureVMSSCommandRequest.getAzureConfig() != null) {
      encryptionService.decrypt(
          azureVMSSCommandRequest.getAzureConfig(), azureVMSSCommandRequest.getAzureEncryptionDetails());
    }

    AzureVMSSTaskHandler handler;
    if (azureVMSSTaskParameters.isSyncTask()) {
      handler = azureVMSSSyncTaskHandler;
    } else {
      switch (azureVMSSTaskParameters.getCommandType()) {
        default: {
          String message = format("Unrecognized task params type running azure vmss task: [%s].",
              azureVMSSTaskParameters.getCommandType().name());
          logger.error(message);
          return AzureVMSSTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
        }
      }
    }
    return handler.executeTask(azureVMSSTaskParameters, azureVMSSCommandRequest.getAzureConfig());
  }
}
