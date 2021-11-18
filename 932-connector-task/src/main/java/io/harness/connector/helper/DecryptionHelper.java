package io.harness.connector.helper;

import io.harness.beans.DecryptableEntity;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface DecryptionHelper {
  DecryptableEntity decrypt(DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptionDetails);
}
