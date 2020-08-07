package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DirectK8sInfraDelegateConfig implements K8sInfraDelegateConfig {
  String namespace;
  KubernetesClusterConfigDTO kubernetesClusterConfigDTO;
  List<EncryptedDataDetail> encryptionDetails;
}
