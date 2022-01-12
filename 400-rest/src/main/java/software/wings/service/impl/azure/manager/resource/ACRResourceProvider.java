/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.azure.manager.resource;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.azure.resource.operation.AzureResourceOperationResponse;
import io.harness.delegate.task.azure.resource.operation.acr.ACRListRepositoryTagsOperation;
import io.harness.delegate.task.azure.resource.operation.acr.ACRListRepositoryTagsOperationResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;

import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDP)
@Singleton
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ACRResourceProvider extends AbstractAzureResourceManager {
  public List<String> listRepositoryTags(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName, String repositoryName) {
    ACRListRepositoryTagsOperation operation = ACRListRepositoryTagsOperation.builder()
                                                   .subscriptionId(subscriptionId)
                                                   .registryName(registryName)
                                                   .repositoryName(repositoryName)
                                                   .build();

    AzureResourceOperationResponse azureResourceOperationResponse =
        executionOperation(azureConfig, encryptionDetails, operation, null);

    return ((ACRListRepositoryTagsOperationResponse) azureResourceOperationResponse).getRepositoryTags();
  }
}
