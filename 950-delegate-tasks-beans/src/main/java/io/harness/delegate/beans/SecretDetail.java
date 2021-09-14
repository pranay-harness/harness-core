/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.security.encryption.EncryptedRecord;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretDetail {
  private String configUuid;
  private EncryptedRecord encryptedRecord;
}
