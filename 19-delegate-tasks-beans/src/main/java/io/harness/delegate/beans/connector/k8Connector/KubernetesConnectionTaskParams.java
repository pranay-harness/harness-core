package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class KubernetesConnectionTaskParams implements TaskParameters {
  KubernetesClusterConfigDTO kubernetesClusterConfig;
  private List<EncryptedDataDetail> encryptionDetails;
}
