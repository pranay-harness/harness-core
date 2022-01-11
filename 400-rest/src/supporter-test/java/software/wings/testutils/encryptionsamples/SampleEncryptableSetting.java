/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.testutils.encryptionsamples;

import io.harness.encryption.Encrypted;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import software.wings.annotation.EncryptableSetting;
import software.wings.settings.SettingVariableTypes;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleEncryptableSetting implements EncryptableSetting, PersistentEntity, UuidAware {
  public static final String ENCRYPTED_ANNOTATION_VALUE_FIELD = "field1";
  public static final String ENCRYPTED_ANNOTATION_KEY_FIELD = "field2";
  private String uuid;
  private String accountId;
  private SettingVariableTypes type;

  @Encrypted(fieldName = ENCRYPTED_ANNOTATION_VALUE_FIELD, isReference = true) private char[] value;
  @SchemaIgnore @FdIndex private String encryptedValue;

  @Encrypted(fieldName = ENCRYPTED_ANNOTATION_KEY_FIELD) private char[] key;
  @SchemaIgnore @FdIndex private String encryptedKey;

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getAccountId() {
    return accountId;
  }

  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public SettingVariableTypes getSettingType() {
    return type;
  }
}
