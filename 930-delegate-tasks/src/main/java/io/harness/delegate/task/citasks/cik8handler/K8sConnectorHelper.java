/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialSpecDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class K8sConnectorHelper {
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;

  public KubernetesConfig getKubernetesConfig(ConnectorDetails k8sConnectorDetails) {
    return getKubernetesConfig((KubernetesClusterConfigDTO) k8sConnectorDetails.getConnectorConfig(),
        k8sConnectorDetails.getEncryptedDataDetails());
  }

  public KubernetesConfig getKubernetesConfig(
      KubernetesClusterConfigDTO clusterConfigDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    KubernetesCredentialSpecDTO credentialSpecDTO = clusterConfigDTO.getCredential().getConfig();
    KubernetesCredentialType kubernetesCredentialType = clusterConfigDTO.getCredential().getKubernetesCredentialType();
    if (kubernetesCredentialType == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesAuthCredentialDTO kubernetesCredentialAuth =
          ((KubernetesClusterDetailsDTO) credentialSpecDTO).getAuth().getCredentials();
      secretDecryptionService.decrypt(kubernetesCredentialAuth, encryptedDataDetails);
    }
    return k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(clusterConfigDTO);
  }
}
