/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.encryptors;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.CUSTOM;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionConfig;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KmsEncryptorsRegistryTest extends SMCoreTestBase {
  @Inject KmsEncryptorsRegistry kmsEncryptorsRegistry;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetKmsEncryptor_shouldReturn() {
    EncryptionConfig encryptionConfig = mock(EncryptionConfig.class);
    when(encryptionConfig.isGlobalKms()).thenReturn(false);
    when(encryptionConfig.getEncryptionType()).thenReturn(GCP_KMS);
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
    assertThat(kmsEncryptor).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetGlobalKmsEncryptor_shouldReturn() {
    EncryptionConfig encryptionConfig = mock(EncryptionConfig.class);
    when(encryptionConfig.isGlobalKms()).thenReturn(true);
    when(encryptionConfig.getEncryptionType()).thenReturn(GCP_KMS);
    KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
    assertThat(kmsEncryptor).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetKmsEncryptor_shouldThrowError() {
    try {
      EncryptionConfig encryptionConfig = mock(EncryptionConfig.class);
      when(encryptionConfig.isGlobalKms()).thenReturn(false);
      when(encryptionConfig.getEncryptionType()).thenReturn(CUSTOM);
      KmsEncryptor kmsEncryptor = kmsEncryptorsRegistry.getKmsEncryptor(encryptionConfig);
      fail("The method should have thrown an error");
    } catch (SecretManagementDelegateException e) {
      assertThat(e.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
  }
}
