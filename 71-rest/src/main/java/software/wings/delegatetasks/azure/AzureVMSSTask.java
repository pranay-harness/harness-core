package software.wings.delegatetasks.azure;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSDeployTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSwitchRouteTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.ServiceVariable;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSDeployTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSRollbackTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSetupTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSwitchRouteTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSyncTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSTaskHandler;
import software.wings.service.impl.azure.manager.AzureVMSSCommandRequest;
import software.wings.service.intfc.security.EncryptionService;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class AzureVMSSTask extends AbstractDelegateRunnableTask {
  @Inject private AzureVMSSSyncTaskHandler azureVMSSSyncTaskHandler;
  @Inject private AzureVMSSSetupTaskHandler setupTaskHandler;
  @Inject private AzureVMSSDeployTaskHandler deployTaskHandler;
  @Inject private AzureVMSSRollbackTaskHandler rollbackTaskHandler;
  @Inject private AzureVMSSSwitchRouteTaskHandler switchRouteTaskHandler;
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

    decryptCommandRequestParameters(azureVMSSCommandRequest);
    decryptTaskParameters(azureVMSSCommandRequest, azureVMSSTaskParameters);

    AzureVMSSTaskHandler handler;
    if (azureVMSSTaskParameters.isSyncTask()) {
      handler = azureVMSSSyncTaskHandler;
    } else {
      switch (azureVMSSTaskParameters.getCommandType()) {
        case AZURE_VMSS_SETUP: {
          handler = setupTaskHandler;
          break;
        }

        case AZURE_VMSS_DEPLOY: {
          if (!(azureVMSSTaskParameters instanceof AzureVMSSDeployTaskParameters)) {
            String message = format("Parameters of unrecognized class: [%s] found while executing deploy step",
                azureVMSSTaskParameters.getClass().getSimpleName());
            logger.error(message);
            return AzureVMSSTaskExecutionResponse.builder()
                .commandExecutionStatus(FAILURE)
                .errorMessage(message)
                .build();
          }
          AzureVMSSDeployTaskParameters deployTaskParameters = (AzureVMSSDeployTaskParameters) azureVMSSTaskParameters;
          handler = deployTaskParameters.isRollback() ? rollbackTaskHandler : deployTaskHandler;
          break;
        }

        case AZURE_VMSS_SWITCH_ROUTE: {
          if (!(azureVMSSTaskParameters instanceof AzureVMSSSwitchRouteTaskParameters)) {
            String message = format("Parameters of unrecognized class: [%s] found while executing deploy step",
                azureVMSSTaskParameters.getClass().getSimpleName());
            logger.error(message);
            return AzureVMSSTaskExecutionResponse.builder()
                .commandExecutionStatus(FAILURE)
                .errorMessage(message)
                .build();
          }

          handler = switchRouteTaskHandler;
          break;
        }

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

  private void decryptCommandRequestParameters(AzureVMSSCommandRequest azureVMSSCommandRequest) {
    if (azureVMSSCommandRequest.getAzureConfig() != null) {
      encryptionService.decrypt(
          azureVMSSCommandRequest.getAzureConfig(), azureVMSSCommandRequest.getAzureEncryptionDetails());
    }
  }

  private void decryptTaskParameters(
      AzureVMSSCommandRequest azureVMSSCommandRequest, AzureVMSSTaskParameters azureVMSSTaskParameters) {
    if (AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SETUP == azureVMSSTaskParameters.getCommandType()) {
      AzureVMSSSetupTaskParameters setupTaskParameters = (AzureVMSSSetupTaskParameters) azureVMSSTaskParameters;

      if (azureVMSSCommandRequest.getHostConnectionAttributes() != null) {
        HostConnectionAttributes hostConnectionAttributes = azureVMSSCommandRequest.getHostConnectionAttributes();
        encryptionService.decrypt(
            hostConnectionAttributes, azureVMSSCommandRequest.getHostConnectionAttributesEncryptionDetails());
        setupTaskParameters.setSshPublicKey(new String(hostConnectionAttributes.getSshPassword()));
      }

      if (azureVMSSCommandRequest.getServiceVariable() != null) {
        ServiceVariable serviceVariable = azureVMSSCommandRequest.getServiceVariable();
        encryptionService.decrypt(serviceVariable, azureVMSSCommandRequest.getServiceVariableEncryptionDetails());
        setupTaskParameters.setPassword(new String(serviceVariable.getValue()));
      }
    }
  }
}
