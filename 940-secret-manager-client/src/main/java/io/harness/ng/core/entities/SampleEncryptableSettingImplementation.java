/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Encrypted;

import software.wings.annotation.EncryptableSetting;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@OwnedBy(PL)
@Data
@Builder
@ToString(exclude = "secretText")
public class SampleEncryptableSettingImplementation implements EncryptableSetting {
  @Encrypted(fieldName = "encryptedId") private String secretText;
  @SchemaIgnore private String encryptedSecretText;

  @Override
  @JsonIgnore
  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.SECRET_TEXT;
  }

  @Override
  @JsonIgnore
  @SchemaIgnore
  public String getAccountId() {
    return "kmpySmUISimoRrJL6NL73w";
  }

  @Override
  public void setAccountId(String accountId) {
    // not required
  }
}
