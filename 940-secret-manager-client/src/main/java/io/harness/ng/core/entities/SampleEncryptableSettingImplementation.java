/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
