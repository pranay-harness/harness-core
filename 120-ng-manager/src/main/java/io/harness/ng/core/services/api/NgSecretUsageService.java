package io.harness.ng.core.services.api;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

public interface NgSecretUsageService {
  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object, String appId, String workflowExecutionId);
}
