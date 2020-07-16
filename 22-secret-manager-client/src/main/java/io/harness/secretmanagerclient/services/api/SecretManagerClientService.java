package io.harness.secretmanagerclient.services.api;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

public interface SecretManagerClientService {
  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object);
}
