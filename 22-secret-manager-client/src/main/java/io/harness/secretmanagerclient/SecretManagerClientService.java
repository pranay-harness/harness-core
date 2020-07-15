package io.harness.secretmanagerclient;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;

import java.util.List;

public interface SecretManagerClientService {
  List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object);
}
