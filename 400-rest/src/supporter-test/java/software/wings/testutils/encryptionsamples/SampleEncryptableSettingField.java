/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.testutils.encryptionsamples;

import io.harness.beans.EncryptedData;
import io.harness.persistence.UuidAware;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleEncryptableSettingField {
  private String accountId;
  private EncryptedData random;
  private UuidAware entity;
}
