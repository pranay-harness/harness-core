package io.harness.secretmanagerclient.services;

import static io.harness.secretmanagerclient.utils.SecretManagerClientUtils.getResponse;

import com.google.inject.Inject;

import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

public class SecretManagerClientServiceImpl implements SecretManagerClientService {
  @Inject private SecretManagerClient secretManagerClient;

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting encryptableSetting) {
    throw new UnsupportedOperationException("This method no longer supported.");
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity consumer) {
    return getResponse(secretManagerClient.getEncryptionDetails(
        NGAccessWithEncryptionConsumer.builder().ngAccess(ngAccess).decryptableEntity(consumer).build()));
  }
}
