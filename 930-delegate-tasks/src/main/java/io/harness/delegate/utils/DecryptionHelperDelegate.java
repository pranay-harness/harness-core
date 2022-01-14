package io.harness.delegate.utils;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.helper.DecryptionHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.List;

public class DecryptionHelperDelegate implements DecryptionHelper {
  @Inject private SecretDecryptionService secretDecryptionService;

  @Override
  public DecryptableEntity decrypt(DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptionDetails) {
    return secretDecryptionService.decrypt(decryptableEntity, encryptionDetails);
  }
}
