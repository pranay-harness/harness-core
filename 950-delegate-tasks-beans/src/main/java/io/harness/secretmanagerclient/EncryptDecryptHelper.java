package io.harness.secretmanagerclient;

import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

public interface EncryptDecryptHelper {
  EncryptedRecord encryptContent(byte[] content, String name, EncryptionConfig config);

  byte[] getDecryptedContent(EncryptionConfig config, EncryptedRecord record);

  boolean deleteEncryptedRecord(EncryptionConfig encryptionConfig, EncryptedRecord record);
}
