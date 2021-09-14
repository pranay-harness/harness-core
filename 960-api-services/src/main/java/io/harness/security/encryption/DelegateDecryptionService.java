/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security.encryption;

import java.util.List;
import java.util.Map;

/**
 * Decrypt a batch of encrypted records. Return a map of the encrypted record UUID to the decrypted secret.
 */
public interface DelegateDecryptionService {
  Map<String, char[]> decrypt(Map<EncryptionConfig, List<EncryptedRecord>> encryptedRecordMap);
}
