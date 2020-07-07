package io.harness.ng.core.services.api.impl;

import static io.harness.ng.core.utils.SecretUtils.getResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.ng.core.services.api.NgSecretUsageService;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretUsageServiceImpl implements NgSecretUsageService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(
      EncryptableSetting object, String appId, String workflowExecutionId) {
    return getResponse(secretManagerClient.getEncryptionDetails(appId, workflowExecutionId, object));
  }
}
