package software.wings.delegatetasks.azure.appservice;

import static io.harness.azure.model.AzureConstants.UNRECOGNIZED_TASK;
import static io.harness.logging.CommandExecutionStatus.FAILURE;

import static java.lang.String.format;

import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;

import software.wings.delegatetasks.azure.AzureSecretHelper;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class AzureAppServiceTask extends AbstractDelegateRunnableTask {
  @Inject private AzureSecretHelper azureSecretHelper;
  @Inject private AzureAppServiceTaskFactory azureAppServiceTaskFactory;

  public AzureAppServiceTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented.");
  }

  @Override
  public AzureTaskExecutionResponse run(TaskParameters parameters) {
    if (!(parameters instanceof AzureTaskExecutionRequest)) {
      String message = format(UNRECOGNIZED_TASK, parameters.getClass().getSimpleName());
      log.error(message);
      return AzureTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    AzureTaskExecutionRequest azureTaskExecutionRequest = (AzureTaskExecutionRequest) parameters;
    AzureConfig azureConfig = azureSecretHelper.decryptAndGetAzureConfig(
        azureTaskExecutionRequest.getAzureConfigDTO(), azureTaskExecutionRequest.getAzureConfigEncryptionDetails());
    ILogStreamingTaskClient logStreamingTaskClient = getLogStreamingTaskClient();

    AzureAppServiceTaskParameters azureAppServiceTaskParameters =
        (AzureAppServiceTaskParameters) azureTaskExecutionRequest.getAzureTaskParameters();
    azureSecretHelper.decryptAzureAppServiceTaskParameters(azureAppServiceTaskParameters);

    AbstractAzureAppServiceTaskHandler azureAppServiceTask =
        azureAppServiceTaskFactory.getAzureAppServiceTask(azureAppServiceTaskParameters.getCommandType().name());

    return azureAppServiceTask.executeTask(azureAppServiceTaskParameters, azureConfig, logStreamingTaskClient);
  }
}
