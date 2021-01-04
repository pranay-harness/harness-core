package software.wings.service.impl.security.kms;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.rule.OwnerRule.TATHAGAT;
import static io.harness.security.encryption.SecretManagerType.VAULT;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.SecretManagerType;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.KmsConfig;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class TerraformPlanEncryptDecryptHelperTest extends WingsBaseTest {
  @Mock private EncryptionConfig encryptionConfig;
  @Mock private EncryptedRecord encryptedRecord;
  @Mock private KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Mock private KmsEncryptor kmsEncryptor;
  @Mock private VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Mock private VaultEncryptor vaultEncryptor;

  @Inject @InjectMocks @Spy private TerraformPlanEncryptDecryptHelper terraformPlanEncryptDecryptHelper;
  private String accountId;
  private String plaintext;
  private final byte[] fileContent = "TerraformPlan".getBytes();
  private final char[] terraformPlan = "TerraformPlan".toCharArray();
  private String encodedTfPlan = encodeBase64(terraformPlan);

  @Before
  public void setup() throws IllegalAccessException {
    when(kmsEncryptorsRegistry.getKmsEncryptor(any())).thenReturn(kmsEncryptor);
    when(vaultEncryptorsRegistry.getVaultEncryptor(any())).thenReturn(vaultEncryptor);
    when(encryptionConfig.getAccountId()).thenReturn(accountId);
    accountId = UUIDGenerator.generateUuid();
    plaintext = UUIDGenerator.generateUuid();
    when(encryptionConfig.getAccountId()).thenReturn(accountId);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testEncryptKmsSecret() {
    when(kmsEncryptor.encryptSecret(accountId, plaintext, encryptionConfig)).thenReturn(encryptedRecord);
    EncryptedRecord record = terraformPlanEncryptDecryptHelper.encryptKmsSecret(plaintext, encryptionConfig);
    assertThat(record).isNotNull();
    assertThat(record).isEqualTo(encryptedRecord);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testEncryptVaultSecret() {
    when(vaultEncryptor.createSecret(accountId, "TerraformPlan", plaintext, encryptionConfig))
        .thenReturn(encryptedRecord);
    EncryptedRecord record =
        terraformPlanEncryptDecryptHelper.encryptVaultSecret("TerraformPlan", plaintext, encryptionConfig);

    assertThat(record).isNotNull();
    assertThat(record).isEqualTo(encryptedRecord);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testEncryptTerraformPlanForKms() {
    when(encryptionConfig.getType()).thenReturn(SecretManagerType.KMS);
    EncryptedRecordData data = EncryptedRecordData.builder().name("data").build();
    doReturn(data).when(terraformPlanEncryptDecryptHelper).encryptKmsSecret(any(), any());
    EncryptedRecord record =
        terraformPlanEncryptDecryptHelper.encryptTerraformPlan(fileContent, "TerraformPlan", encryptionConfig);

    verify(terraformPlanEncryptDecryptHelper, times(1)).encryptKmsSecret(any(), any());
    assertThat(record).isNotNull();
    assertThat(record).isEqualTo(data);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testEncryptTerraformPlanForVault() {
    when(encryptionConfig.getType()).thenReturn(VAULT);
    EncryptedRecordData data = EncryptedRecordData.builder().name("data").build();
    doReturn(data).when(terraformPlanEncryptDecryptHelper).encryptVaultSecret(any(), any(), any());
    EncryptedRecord record =
        terraformPlanEncryptDecryptHelper.encryptTerraformPlan(fileContent, "TerraformPlan", encryptionConfig);

    verify(terraformPlanEncryptDecryptHelper, times(1)).encryptVaultSecret(any(), any(), any());
    assertThat(record).isNotNull();
    assertThat(record).isEqualTo(data);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testEncryptTerraformPlanUnsupportedSecretManager() {
    when(encryptionConfig.getType()).thenReturn(null);
    assertThatThrownBy(
        () -> terraformPlanEncryptDecryptHelper.encryptTerraformPlan(fileContent, "TerraformPlan", encryptionConfig))
        .isInstanceOf(SecretManagementDelegateException.class)
        .hasMessage("Encryptor for fetch secret task for encryption config null not configured");
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testFetchKmsSecretValue() {
    when(kmsEncryptor.fetchSecretValue(accountId, encryptedRecord, encryptionConfig)).thenReturn(terraformPlan);
    char[] decryptedPlan = terraformPlanEncryptDecryptHelper.fetchKmsSecretValue(encryptedRecord, encryptionConfig);
    assertThat(decryptedPlan).isNotNull();
    assertThat(decryptedPlan).isEqualTo(terraformPlan);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testFetchVaultSecretValue() {
    when(vaultEncryptor.fetchSecretValue(accountId, encryptedRecord, encryptionConfig)).thenReturn(terraformPlan);
    char[] decryptedPlan = terraformPlanEncryptDecryptHelper.fetchVaultSecretValue(encryptedRecord, encryptionConfig);
    assertThat(decryptedPlan).isNotNull();
    assertThat(decryptedPlan).isEqualTo(terraformPlan);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetDecryptedTerraformPlanFromKms() {
    when(encryptionConfig.getType()).thenReturn(SecretManagerType.KMS);
    doReturn(encodedTfPlan.toCharArray()).when(terraformPlanEncryptDecryptHelper).fetchKmsSecretValue(any(), any());
    byte[] result = terraformPlanEncryptDecryptHelper.getDecryptedTerraformPlan(encryptionConfig, encryptedRecord);
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(fileContent);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetDecryptedTerraformPlanFromVault() {
    when(encryptionConfig.getType()).thenReturn(VAULT);
    doReturn(encodedTfPlan.toCharArray()).when(terraformPlanEncryptDecryptHelper).fetchVaultSecretValue(any(), any());
    byte[] result = terraformPlanEncryptDecryptHelper.getDecryptedTerraformPlan(encryptionConfig, encryptedRecord);
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(fileContent);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetDecryptedTerraformPlanUnsupportedSecretManager() {
    when(encryptionConfig.getType()).thenReturn(null);
    assertThatThrownBy(
        () -> terraformPlanEncryptDecryptHelper.getDecryptedTerraformPlan(encryptionConfig, encryptedRecord))
        .isInstanceOf(SecretManagementDelegateException.class)
        .hasMessage("Encryptor for fetch secret task for encryption config null not configured");
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testdeleteTfPlanFromVaultWithAwsSecretManager() {
    doReturn(vaultEncryptor).when(vaultEncryptorsRegistry).getVaultEncryptor(any());
    doReturn(true).when(vaultEncryptor).deleteSecret(any(), any(), any());
    boolean isPlanDeleted = terraformPlanEncryptDecryptHelper.deleteTfPlanFromVault(
        AwsSecretsManagerConfig.builder().accountId(ACCOUNT_ID).build(), encryptedRecord);
    verify(vaultEncryptor, times(1)).deleteSecret(any(), any(), any());
    assertThat(isPlanDeleted).isTrue();
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testdeleteTfPlanFromVaultWithKms() {
    boolean isPlanDeleted = terraformPlanEncryptDecryptHelper.deleteTfPlanFromVault(
        KmsConfig.builder().accountId(ACCOUNT_ID).build(), encryptedRecord);
    verify(vaultEncryptor, never()).deleteSecret(any(), any(), any());
    verify(terraformPlanEncryptDecryptHelper, never()).fetchVaultSecretValue(any(), any());
    assertThat(isPlanDeleted).isFalse();
  }
}