package software.wings.service.intfc.security;

import software.wings.beans.LocalEncryptionConfig;
import software.wings.security.encryption.EncryptedData;

/**
 * @author marklu on 2019-05-14
 */
public interface LocalEncryptionService {
  EncryptedData encrypt(char[] value, String accountId, LocalEncryptionConfig localEncryptionConfig);

  char[] decrypt(EncryptedData data, String accountId, LocalEncryptionConfig localEncryptionConfig);

  LocalEncryptionConfig getEncryptionConfig(String accountId);
}
