package io.harness.connector;

import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public class DecryptableEntityHelper {
  public DecryptableEntityWithEncryptionConsumers buildDecryptableEntityWithEncryptionConsumers(
      DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptedDataDetails) {
    return DecryptableEntityWithEncryptionConsumers.builder()
        .decryptableEntity(decryptableEntity)
        .encryptedDataDetailList(encryptedDataDetails)
        .build();
  }
}
