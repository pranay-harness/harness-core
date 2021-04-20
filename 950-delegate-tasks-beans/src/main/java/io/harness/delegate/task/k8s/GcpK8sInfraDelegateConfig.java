package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class GcpK8sInfraDelegateConfig implements K8sInfraDelegateConfig {
  String namespace;
  String cluster;
  GcpConnectorDTO gcpConnectorDTO;
  List<EncryptedDataDetail> encryptionDataDetails;
}
