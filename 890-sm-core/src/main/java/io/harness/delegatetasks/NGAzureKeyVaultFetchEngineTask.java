/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.SecretManagementDelegateException;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NGAzureKeyVaultFetchEngineTask extends AbstractDelegateRunnableTask {
  public NGAzureKeyVaultFetchEngineTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return run((NGAzureKeyVaultFetchEngineTaskParameters) parameters[0]);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    return run((NGAzureKeyVaultFetchEngineTaskParameters) parameters);
  }

  private NGAzureKeyVaultFetchEngineResponse run(
      NGAzureKeyVaultFetchEngineTaskParameters azureKeyVaultFetchEngineTaskParameters) {
    List<String> secretEngines;
    try {
      secretEngines = listVaultsInternal(azureKeyVaultFetchEngineTaskParameters.getAzureKeyVaultConnectorDTO());
    } catch (IOException exception) {
      String message = "Failed to list secret engines for due to unexpected network error. Please try again.";
      log.error(message, exception);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, USER);
    }
    return NGAzureKeyVaultFetchEngineResponse.builder().secretEngines(secretEngines).build();
  }

  private List<String> listVaultsInternal(AzureKeyVaultConnectorDTO azureKeyVaultConnectorDTO) throws IOException {
    Azure azure;
    List<Vault> vaultList = new ArrayList<>();
    ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureKeyVaultConnectorDTO.getClientId(),
        azureKeyVaultConnectorDTO.getTenantId(),
        String.valueOf(azureKeyVaultConnectorDTO.getSecretKey().getDecryptedValue()),
        getAzureEnvironment(azureKeyVaultConnectorDTO.getAzureEnvironmentType()));

    Azure.Authenticated authenticate = Azure.configure().authenticate(credentials);

    if (isEmpty(azureKeyVaultConnectorDTO.getSubscription())) {
      azure = authenticate.withDefaultSubscription();
    } else {
      azure = authenticate.withSubscription(azureKeyVaultConnectorDTO.getSubscription());
    }

    for (ResourceGroup rGroup : azure.resourceGroups().list()) {
      vaultList.addAll(azure.vaults().listByResourceGroup(rGroup.name()));
    }

    return vaultList.stream().map(HasName::name).collect(Collectors.toList());
  }

  private AzureEnvironment getAzureEnvironment(AzureEnvironmentType azureEnvironmentType) {
    if (azureEnvironmentType == null) {
      return AzureEnvironment.AZURE;
    }

    switch (azureEnvironmentType) {
      case AZURE_US_GOVERNMENT:
        return AzureEnvironment.AZURE_US_GOVERNMENT;

      case AZURE:
      default:
        return AzureEnvironment.AZURE;
    }
  }
}
