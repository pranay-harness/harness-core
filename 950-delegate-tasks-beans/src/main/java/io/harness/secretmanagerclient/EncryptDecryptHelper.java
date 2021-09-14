/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.secretmanagerclient;

import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

public interface EncryptDecryptHelper {
  EncryptedRecord encryptContent(byte[] content, String name, EncryptionConfig config);

  byte[] getDecryptedContent(EncryptionConfig config, EncryptedRecord record);

  boolean deleteEncryptedRecord(EncryptionConfig encryptionConfig, EncryptedRecord record);
}
