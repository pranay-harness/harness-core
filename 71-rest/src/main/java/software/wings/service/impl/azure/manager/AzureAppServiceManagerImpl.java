package software.wings.service.impl.azure.manager;

import static io.harness.azure.model.AzureConstants.DEFAULT_SYNC_AZURE_VMSS_TIMEOUT_MIN;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.CommandExecutionStatus.FAILURE;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import static java.lang.String.format;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppDeploymentSlotNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppListWebAppNamesParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppDeploymentSlotNamesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppNamesResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;

import software.wings.beans.AzureConfig;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.azure.manager.AzureAppServiceManager;
import software.wings.sm.states.azure.AzureStateHelper;

import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AzureAppServiceManagerImpl implements AzureAppServiceManager {
  @Inject private DelegateService delegateService;
  @Inject private transient AzureStateHelper azureStateHelper;

  @Override
  public List<String> getAppServiceNamesByResourceGroup(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String appId, String subscriptionId, String resourceGroup,
      String appType) {
    AzureWebAppListWebAppNamesParameters parameters = AzureWebAppListWebAppNamesParameters.builder()
                                                          .subscriptionId(subscriptionId)
                                                          .resourceGroupName(resourceGroup)
                                                          .appServiceType(appType)
                                                          .build();
    AzureTaskResponse azureTaskExecutionResponse = executeTask(parameters, azureConfig, encryptionDetails, appId);
    return ((AzureWebAppListWebAppNamesResponse) azureTaskExecutionResponse).getWebAppNames();
  }

  @Override
  public List<String> getAppServiceDeploymentSlotNames(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String appId, String subscriptionId, String resourceGroup,
      String appType, String appName) {
    AzureWebAppListWebAppDeploymentSlotNamesParameters parameters =
        AzureWebAppListWebAppDeploymentSlotNamesParameters.builder()
            .subscriptionId(subscriptionId)
            .resourceGroupName(resourceGroup)
            .appServiceType(appType)
            .appName(appName)
            .build();
    AzureTaskResponse azureTaskExecutionResponse = executeTask(parameters, azureConfig, encryptionDetails, appId);
    return ((AzureWebAppListWebAppDeploymentSlotNamesResponse) azureTaskExecutionResponse).getDeploymentSlotNames();
  }

  private AzureTaskResponse executeTask(AzureAppServiceTaskParameters parameters, AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String appId) {
    AzureTaskExecutionRequest request = AzureTaskExecutionRequest.builder()
                                            .azureConfigDTO(azureStateHelper.createAzureConfigDTO(azureConfig))
                                            .azureConfigEncryptionDetails(encryptionDetails)
                                            .azureTaskParameters(parameters)
                                            .build();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(azureConfig.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
            .data(TaskData.builder()
                      .async(false)
                      .taskType(TaskType.AZURE_APP_SERVICE_TASK.name())
                      .parameters(new Object[] {request})
                      .timeout(TimeUnit.MINUTES.toMillis(DEFAULT_SYNC_AZURE_VMSS_TIMEOUT_MIN))
                      .build())
            .build();
    try {
      DelegateResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      if (notifyResponseData instanceof ErrorNotifyResponseData) {
        throw new InvalidRequestException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage(), USER);
      } else if (notifyResponseData instanceof RemoteMethodReturnValueData) {
        throw new InvalidRequestException(
            getMessage(((RemoteMethodReturnValueData) notifyResponseData).getException()), USER);
      } else if (!(notifyResponseData instanceof AzureTaskExecutionResponse)) {
        throw new InvalidRequestException(
            format("Unknown response from delegate: [%s]", notifyResponseData.getClass().getSimpleName()), USER);
      }
      AzureTaskExecutionResponse response = (AzureTaskExecutionResponse) notifyResponseData;
      if (FAILURE == response.getCommandExecutionStatus()) {
        throw new InvalidRequestException(response.getErrorMessage());
      }
      return response.getAzureTaskResponse();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(ex.getMessage(), ex, USER);
    }
  }
}
