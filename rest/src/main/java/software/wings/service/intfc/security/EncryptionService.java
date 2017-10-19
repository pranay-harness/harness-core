package software.wings.service.intfc.security;

import software.wings.beans.KmsConfig;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;

import java.io.IOException;

/**
 * Created by rsingh on 10/18/17.
 */
public interface EncryptionService {
  char[] decrypt(EncryptionType encryptionType, String accountId, EncryptedData encryptedData, KmsConfig kmsConfig)
      throws IOException;
}
