package io.harness.secretmanagerclient.services;

import static io.harness.secretmanagerclient.utils.SecretManagerClientUtils.getResponse;

import com.google.inject.Inject;

import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
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
