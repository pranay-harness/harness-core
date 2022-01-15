/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.encryptors;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.helpers.LocalEncryptorHelper;
import io.harness.rule.Owner;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedMech;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import software.wings.beans.LocalEncryptionConfig;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PL)
public class LocalEncryptorTest extends SMCoreTestBase {
  @Mock private FeatureFlagHelperService featureFlagHelperService;
  @InjectMocks @Inject private LocalEncryptor localEncryptor;
  @InjectMocks @Inject private LocalEncryptorHelper localEncryptorHelper;

  private static final String ACCOUNTID = "accountId";

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testEncryptDecrypt() {
    String valueToEncrypt = "value";
    SecretManagerConfig secretManagerConfig = getLocalEncryptionConfig();
    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(ACCOUNTID, valueToEncrypt, secretManagerConfig);

    String decryptedValue =
        new String(localEncryptor.fetchSecretValue(ACCOUNTID, encryptedRecord, secretManagerConfig));
    assertThat(decryptedValue).isEqualTo(valueToEncrypt);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testEncryptDecryptInMultiCryptoMode() {
    when(featureFlagHelperService.isEnabled(ACCOUNTID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);

    SecretManagerConfig secretManagerConfig = getLocalEncryptionConfig();
    String valueToEncrypt = "value";
    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(ACCOUNTID, valueToEncrypt, secretManagerConfig);

    assertThat(encryptedRecord.getEncryptedMech()).isEqualTo(EncryptedMech.MULTI_CRYPTO);
    assertThat(encryptedRecord.getEncryptedValue()).isNotNull();
    assertThat(encryptedRecord.getEncryptionKey()).isNotNull();
    assertThat(encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.SECRET_KEY_UUID_KEY))
        .isNotNull();
    assertThat(encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.AWS_ENCRYPTED_SECRET))
        .isNotNull();

    String decryptedValue =
        new String(localEncryptor.fetchSecretValue(ACCOUNTID, encryptedRecord, secretManagerConfig));
    assertThat(decryptedValue).isEqualTo(valueToEncrypt);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testEncryptDecryptAwsSdkMode() {
    when(featureFlagHelperService.isEnabled(ACCOUNTID, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)).thenReturn(true);

    SecretManagerConfig secretManagerConfig = getLocalEncryptionConfig();
    String valueToEncrypt = "value";
    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(ACCOUNTID, valueToEncrypt, secretManagerConfig);

    assertThat(encryptedRecord.getEncryptedMech()).isEqualTo(EncryptedMech.AWS_ENCRYPTION_SDK_CRYPTO);
    assertThat(encryptedRecord.getEncryptionKey()).isNotNull();
    assertThat(encryptedRecord.getAdditionalMetadata()).isNull();
    assertThat(encryptedRecord.getEncryptedValue()).isNull();
    assertThat(encryptedRecord.getEncryptedValueBytes()).isNotNull();

    String decryptedValue =
        new String(localEncryptor.fetchSecretValue(ACCOUNTID, encryptedRecord, secretManagerConfig));
    assertThat(decryptedValue).isEqualTo(valueToEncrypt);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testEncryptMultiCryptoModeThenDecryptWithoutIt() {
    when(featureFlagHelperService.isEnabled(ACCOUNTID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);

    SecretManagerConfig secretManagerConfig = getLocalEncryptionConfig();
    String valueToEncrypt = "value";
    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(ACCOUNTID, valueToEncrypt, secretManagerConfig);

    when(featureFlagHelperService.isEnabled(ACCOUNTID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(false);
    String decryptedValue =
        new String(localEncryptor.fetchSecretValue(ACCOUNTID, encryptedRecord, secretManagerConfig));
    assertThat(decryptedValue).isEqualTo(valueToEncrypt);
  }

  private SecretManagerConfig getLocalEncryptionConfig() {
    SecretManagerConfig secretManagerConfig = LocalEncryptionConfig.builder().accountId(ACCOUNTID).build();
    localEncryptorHelper.populateConfigForEncryption(secretManagerConfig);
    return secretManagerConfig;
  }
}
