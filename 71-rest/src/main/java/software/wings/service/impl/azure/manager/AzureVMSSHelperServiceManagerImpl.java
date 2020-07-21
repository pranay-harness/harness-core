package software.wings.service.impl.azure.manager;

import static io.harness.azure.model.AzureConstants.defaultSyncAzureVMSSTimeoutMin;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.azure.model.SubscriptionData;
import io.harness.azure.model.VirtualMachineScaleSetData;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.azure.request.AzureVMSSGetVirtualMachineScaleSetParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListResourceGroupsNamesParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListSubscriptionsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSListVirtualMachineScaleSetsParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.delegate.task.azure.response.AzureVMSSGetVirtualMachineScaleSetResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListResourceGroupsNamesResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListSubscriptionsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSListVirtualMachineScaleSetsResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureVMSSTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.Cd1SetupFields;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.azure.manager.AzureVMSSHelperServiceManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class AzureVMSSHelperServiceManagerImpl implements AzureVMSSHelperServiceManager {
  @Inject private DelegateService delegateService;

  @Override
  public List<SubscriptionData> listSubscriptions(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String appId) {
    AzureVMSSTaskResponse response = executeTask(azureConfig.getAccountId(),
        AzureVMSSListSubscriptionsParameters.builder().build(), azureConfig, encryptionDetails, appId);

    return ((AzureVMSSListSubscriptionsResponse) response).getSubscriptions();
  }

  @Override
  public List<String> listResourceGroupsNames(
      AzureConfig azureConfig, String subscriptionId, List<EncryptedDataDetail> encryptionDetails, String appId) {
    AzureVMSSTaskResponse response = executeTask(azureConfig.getAccountId(),
        AzureVMSSListResourceGroupsNamesParameters.builder().subscriptionId(subscriptionId).build(), azureConfig,
        encryptionDetails, appId);

    return ((AzureVMSSListResourceGroupsNamesResponse) response).getResourceGroupsNames();
  }

  @Override
  public List<VirtualMachineScaleSetData> listVirtualMachineScaleSets(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, List<EncryptedDataDetail> encryptionDetails, String appId) {
    AzureVMSSTaskResponse response = executeTask(azureConfig.getAccountId(),
        AzureVMSSListVirtualMachineScaleSetsParameters.builder()
            .resourceGroupName(resourceGroupName)
            .subscriptionId(subscriptionId)
            .build(),
        azureConfig, encryptionDetails, appId);

    return ((AzureVMSSListVirtualMachineScaleSetsResponse) response).getVirtualMachineScaleSets();
  }

  @Override
  public VirtualMachineScaleSetData getVirtualMachineScaleSet(AzureConfig azureConfig, String subscriptionId,
      String resourceGroupName, String vmssName, List<EncryptedDataDetail> encryptionDetails, String appId) {
    AzureVMSSTaskResponse response = executeTask(azureConfig.getAccountId(),
        AzureVMSSGetVirtualMachineScaleSetParameters.builder()
            .subscriptionId(subscriptionId)
            .resourceGroupName(resourceGroupName)
            .vmssName(vmssName)
            .build(),
        azureConfig, encryptionDetails, appId);

    return ((AzureVMSSGetVirtualMachineScaleSetResponse) response).getVirtualMachineScaleSet();
  }

  private AzureVMSSTaskResponse executeTask(String accountId, AzureVMSSTaskParameters parameters,
      AzureConfig azureConfig, List<EncryptedDataDetail> azureEncryptionDetails, String appId) {
    AzureVMSSCommandRequest request = AzureVMSSCommandRequest.builder()
                                          .azureConfig(azureConfig)
                                          .azureEncryptionDetails(azureEncryptionDetails)
                                          .azureVMSSTaskParameters(parameters)
                                          .build();

    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
            .data(TaskData.builder()
                      .async(false)
                      .taskType(TaskType.AZURE_VMSS_COMMAND_TASK.name())
                      .parameters(new Object[] {request})
                      .timeout(TimeUnit.MINUTES.toMillis(defaultSyncAzureVMSSTimeoutMin))
                      .build())
            .build();
    try {
      ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      if (notifyResponseData instanceof ErrorNotifyResponseData) {
        throw new InvalidRequestException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage(), USER);
      } else if (notifyResponseData instanceof RemoteMethodReturnValueData) {
        throw new InvalidRequestException(
            getMessage(((RemoteMethodReturnValueData) notifyResponseData).getException()), USER);
      } else if (!(notifyResponseData instanceof AzureVMSSTaskExecutionResponse)) {
        throw new InvalidRequestException(
            format("Unknown response from delegate: [%s]", notifyResponseData.getClass().getSimpleName()), USER);
      }
      AzureVMSSTaskExecutionResponse response = (AzureVMSSTaskExecutionResponse) notifyResponseData;
      if (FAILURE == response.getCommandExecutionStatus()) {
        throw new InvalidRequestException(response.getErrorMessage());
      }
      return response.getAzureVMSSTaskResponse();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(ex.getMessage(), ex, USER);
    }
  }
}
