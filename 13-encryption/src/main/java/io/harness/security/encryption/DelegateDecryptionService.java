package io.harness.security.encryption;

import software.wings.service.intfc.security.EncryptionConfig;

import java.util.List;
import java.util.Map;

/**
 * Decrypt a batch of encrypted records. Return a map of the encrypted record UUID to the decrypted secret.
 */
public interface DelegateDecryptionService {
  Map<String, char[]> decrypt(Map<EncryptionConfig, List<EncryptedRecord>> encryptedRecordMap);
}
