package io.harness.encryptors;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.rule.Owner;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedMech;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class LocalEncryptorTest extends SMCoreTestBase {
  @InjectMocks @Inject private LocalEncryptor localEncryptor;
  @Mock private FeatureFlagHelperService featureFlagHelperService;

  private static final String ACCOUNTID = "accountId";

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testEncryptDecrypt() {
    String valueToEncrypt = "value";
    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(ACCOUNTID, valueToEncrypt, null);

    String decryptedValue = new String(localEncryptor.fetchSecretValue(ACCOUNTID, encryptedRecord, null));
    assertThat(decryptedValue).isEqualTo(valueToEncrypt);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testEncryptDecryptInMultiCryptoMode() {
    when(featureFlagHelperService.isEnabled(ACCOUNTID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);

    String valueToEncrypt = "value";
    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(ACCOUNTID, valueToEncrypt, null);

    assertThat(encryptedRecord.getEncryptedMech()).isEqualTo(EncryptedMech.MULTI_CRYPTO);
    assertThat(encryptedRecord.getEncryptedValue()).isNull();
    assertThat(encryptedRecord.getEncryptionKey()).isNotNull();
    assertThat(encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.SECRET_KEY_KEY)).isNotNull();
    assertThat(encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.AWS_ENCRYPTED_SECRET))
        .isNotNull();

    String decryptedValue = new String(localEncryptor.fetchSecretValue(ACCOUNTID, encryptedRecord, null));
    assertThat(decryptedValue).isEqualTo(valueToEncrypt);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testEncryptDecryptAwsSdkMode() {
    when(featureFlagHelperService.isEnabled(ACCOUNTID, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)).thenReturn(true);

    String valueToEncrypt = "value";
    EncryptedRecord encryptedRecord = localEncryptor.encryptSecret(ACCOUNTID, valueToEncrypt, null);

    assertThat(encryptedRecord.getEncryptedMech()).isEqualTo(EncryptedMech.AWS_ENCRYPTION_SDK_CRYPTO);
    assertThat(encryptedRecord.getEncryptionKey()).isNotNull();
    assertThat(encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.SECRET_KEY_KEY)).isNull();
    assertThat(encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.AWS_ENCRYPTED_SECRET))
        .isNull();
    assertThat(encryptedRecord.getEncryptedValue()).isNull();
    assertThat(encryptedRecord.getEncryptedValueBytes()).isNotNull();

    String decryptedValue = new String(localEncryptor.fetchSecretValue(ACCOUNTID, encryptedRecord, null));
    assertThat(decryptedValue).isEqualTo(valueToEncrypt);
  }
}
