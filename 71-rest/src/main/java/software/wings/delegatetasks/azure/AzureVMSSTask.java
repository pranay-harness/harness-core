package software.wings.delegatetasks.azure;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.azure.AzureConfigDelegate;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSDeployTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSetupTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSSwitchRouteTaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSDeployTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSRollbackTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSetupTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSwitchRouteTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSyncTaskHandler;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSTaskHandler;
import software.wings.service.impl.azure.manager.AzureVMSSCommandRequest;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class AzureVMSSTask extends AbstractDelegateRunnableTask {
  @Inject private AzureVMSSSyncTaskHandler azureVMSSSyncTaskHandler;
  @Inject private AzureVMSSSetupTaskHandler setupTaskHandler;
  @Inject private AzureVMSSDeployTaskHandler deployTaskHandler;
  @Inject private AzureVMSSRollbackTaskHandler rollbackTaskHandler;
  @Inject private AzureVMSSSwitchRouteTaskHandler switchRouteTaskHandler;
  @Inject private SecretDecryptionService secretDecryptionService;

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
    decryptTaskParameters(azureVMSSCommandRequest, azureVMSSTaskParameters);
    AzureConfig azureConfigForDelegateTask = createAzureConfigForDelegateTask(azureVMSSCommandRequest);
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
    return handler.executeTask(azureVMSSTaskParameters, azureConfigForDelegateTask);
  }

  private AzureConfig createAzureConfigForDelegateTask(AzureVMSSCommandRequest azureVMSSCommandRequest) {
    AzureConfigDelegate azureConfigDelegate = azureVMSSCommandRequest.getAzureConfigDelegate();
    AzureConfigDTO azureConfigDTO = azureConfigDelegate.getAzureConfigDTO();
    secretDecryptionService.decrypt(azureConfigDTO, azureConfigDelegate.getAzureEncryptionDetails());

    String clientId = azureConfigDTO.getClientId();
    String tenantId = azureConfigDTO.getTenantId();
    char[] key = azureConfigDTO.getKey();
    return AzureConfig.builder().clientId(clientId).tenantId(tenantId).key(key).build();
  }

  private void decryptTaskParameters(
      AzureVMSSCommandRequest azureVMSSCommandRequest, AzureVMSSTaskParameters azureVMSSTaskParameters) {
    if (AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SETUP == azureVMSSTaskParameters.getCommandType()) {
      AzureVMSSSetupTaskParameters setupTaskParameters = (AzureVMSSSetupTaskParameters) azureVMSSTaskParameters;

      if (azureVMSSCommandRequest.getAzureHostConnectionDelegate() != null
          && azureVMSSCommandRequest.getAzureHostConnectionDelegate().getAzureVMAuthDTO() != null) {
        AzureVMAuthDTO azureVMAuthDTO = azureVMSSCommandRequest.getAzureHostConnectionDelegate().getAzureVMAuthDTO();
        List<EncryptedDataDetail> azureEncryptionDetails =
            azureVMSSCommandRequest.getAzureHostConnectionDelegate().getAzureEncryptionDetails();
        secretDecryptionService.decrypt(azureVMAuthDTO, azureEncryptionDetails);
        setupTaskParameters.setSshPublicKey(String.valueOf(azureVMAuthDTO.getKey()));
      }

      if (azureVMSSCommandRequest.getAzureVMCredentialsDelegate() != null
          && azureVMSSCommandRequest.getAzureVMCredentialsDelegate().getAzureVMAuthDTO() != null) {
        AzureVMAuthDTO azureVMAuthDTO = azureVMSSCommandRequest.getAzureVMCredentialsDelegate().getAzureVMAuthDTO();
        List<EncryptedDataDetail> azureEncryptionDetails =
            azureVMSSCommandRequest.getAzureVMCredentialsDelegate().getAzureEncryptionDetails();
        secretDecryptionService.decrypt(azureVMAuthDTO, azureEncryptionDetails);
        setupTaskParameters.setPassword(String.valueOf(azureVMAuthDTO.getKey()));
      }
    }
  }
}
