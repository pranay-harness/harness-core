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
import io.harness.delegate.task.azure.resource.operation.k8s.AzureKubernetesClusterDTO;
import io.harness.delegate.task.azure.resource.operation.k8s.AzureListKubernetesClustersOperation;
import io.harness.delegate.task.azure.resource.operation.k8s.AzureListKubernetesClustersOperationResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;
import software.wings.beans.AzureKubernetesCluster;

import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(CDP)
@Singleton
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class AzureK8sResourceProvider extends AbstractAzureResourceManager {
  public List<AzureKubernetesCluster> listKubernetesClusters(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    AzureListKubernetesClustersOperation operationRequest =
        AzureListKubernetesClustersOperation.builder().subscriptionId(subscriptionId).build();

    AzureResourceOperationResponse azureResourceOperationResponse =
        executionOperation(azureConfig, encryptionDetails, operationRequest, null);

    AzureListKubernetesClustersOperationResponse k8sClustersList =
        (AzureListKubernetesClustersOperationResponse) azureResourceOperationResponse;

    return k8sClustersList.getKubernetesClusterList()
        .stream()
        .map(this::toAzureKubernetesCluster)
        .collect(Collectors.toList());
  }

  private AzureKubernetesCluster toAzureKubernetesCluster(AzureKubernetesClusterDTO kubernetesClusterDTO) {
    return AzureKubernetesCluster.builder()
        .name(kubernetesClusterDTO.getName())
        .resourceGroup(kubernetesClusterDTO.getResourceGroup())
        .subscriptionId(kubernetesClusterDTO.getSubscriptionId())
        .type(kubernetesClusterDTO.getType())
        .id(kubernetesClusterDTO.getId())
        .build();
  }
}
