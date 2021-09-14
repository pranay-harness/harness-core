/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.encryptors;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecord;

import software.wings.beans.LocalEncryptionConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LocalEncryptorTest extends CategoryTest {
  private LocalEncryptor localEncryptor;
  private LocalEncryptionConfig localEncryptionConfig;

  @Before
  public void setup() {
    localEncryptor = new LocalEncryptor();
    localEncryptionConfig = LocalEncryptionConfig.builder().accountId(UUIDGenerator.generateUuid()).build();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptDecrypt() {
    String plainTextValue = UUIDGenerator.generateUuid();
    EncryptedRecord encryptedRecord =
        localEncryptor.encryptSecret(localEncryptionConfig.getAccountId(), plainTextValue, localEncryptionConfig);
    assertThat(encryptedRecord).isNotNull();
    char[] returnedValue =
        localEncryptor.fetchSecretValue(localEncryptionConfig.getAccountId(), encryptedRecord, localEncryptionConfig);
    assertThat(returnedValue).isEqualTo(plainTextValue.toCharArray());
  }
}
