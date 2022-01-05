package io.harness.secretmanagerclient;

import io.harness.delegate.beans.DelegateFile;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import java.io.IOException;

public interface EncryptDecryptHelper {
  EncryptedRecord encryptContent(byte[] content, String name, EncryptionConfig config);

  EncryptedRecord encryptFile(byte[] content, String name, EncryptionConfig config, DelegateFile delegateFile)
      throws IOException;

  byte[] getDecryptedContent(EncryptionConfig config, EncryptedRecord record);

  byte[] getDecryptedContent(EncryptionConfig config, EncryptedRecord record, String accountId) throws IOException;

  boolean deleteEncryptedRecord(EncryptionConfig encryptionConfig, EncryptedRecord record);
}
