package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

@OwnedBy(CDP)
public interface K8sInfraDelegateConfig {
  String getNamespace();
  List<EncryptedDataDetail> getEncryptionDataDetails();
}
