package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DirectK8sInfraDelegateConfig implements K8sInfraDelegateConfig {
  String namespace;
  KubernetesClusterConfigDTO kubernetesClusterConfigDTO;
  List<EncryptedDataDetail> encryptionDataDetails;
}
