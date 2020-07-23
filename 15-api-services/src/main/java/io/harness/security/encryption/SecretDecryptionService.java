package io.harness.security.encryption;

import software.wings.annotation.EncryptableSetting;

import java.io.IOException;
import java.util.List;

public interface SecretDecryptionService {
  EncryptableSetting decrypt(EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails);

  List<EncryptableSettingWithEncryptionDetails> decrypt(
      List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetailsList);

  char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException;
}
