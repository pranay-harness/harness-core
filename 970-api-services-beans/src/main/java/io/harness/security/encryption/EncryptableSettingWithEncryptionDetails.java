/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security.encryption;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;

import software.wings.annotation.EncryptableSetting;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncryptableSettingWithEncryptionDetails {
  // This generated UUID is for correlating the decrypted data details with the input details.
  @Default private String detailId = UUIDGenerator.generateUuid();
  private EncryptableSetting encryptableSetting;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
