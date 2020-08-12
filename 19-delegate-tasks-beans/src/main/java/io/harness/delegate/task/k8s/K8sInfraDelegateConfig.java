package io.harness.delegate.task.k8s;

import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface K8sInfraDelegateConfig {
  String getNamespace();
  List<EncryptedDataDetail> getEncryptionDataDetails();
}
