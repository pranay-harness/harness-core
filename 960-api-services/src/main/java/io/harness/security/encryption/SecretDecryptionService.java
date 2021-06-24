package io.harness.security.encryption;

import io.harness.beans.DecryptableEntity;

import software.wings.annotation.EncryptableSetting;

import java.io.IOException;
import java.util.List;

public interface SecretDecryptionService {
  DecryptableEntity decrypt(DecryptableEntity object, List<EncryptedDataDetail> encryptedDataDetails);

  char[] getDecryptedValue(EncryptedDataDetail encryptedDataDetail) throws IOException;

  EncryptableSetting decrypt(
      EncryptableSetting object, List<EncryptedDataDetail> encryptedDataDetails, boolean fromCache);
}
