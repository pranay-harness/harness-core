package io.harness.ng.core.services.api.impl;

import static io.harness.ng.core.utils.SecretUtils.getResponse;

import com.google.inject.Inject;

import io.harness.ng.core.remote.client.rest.factory.SecretManagerClient;
import io.harness.secretmanagerclient.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

public class SecretManagerClientServiceImpl implements SecretManagerClientService {
  @Inject private SecretManagerClient secretManagerClient;

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object) {
    return getResponse(secretManagerClient.getEncryptionDetails(object));
  }
}
