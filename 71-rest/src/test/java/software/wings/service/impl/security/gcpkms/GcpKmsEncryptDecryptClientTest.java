package software.wings.service.impl.security.gcpkms;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.protobuf.ByteString;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.beans.GcpKmsConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementDelegateException;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.nio.charset.StandardCharsets;

@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest(KeyManagementServiceClient.class)
@PowerMockIgnore({"javax.security.*", "org.apache.http.conn.ssl.", "javax.net.ssl.", "javax.crypto.*"})
public class GcpKmsEncryptDecryptClientTest extends CategoryTest {
  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient = spy(new GcpKmsEncryptDecryptClient(timeLimiter));
  private GcpKmsConfig gcpKmsConfig;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptDecrypt() {
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();
    gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setUuid(UUIDGenerator.generateUuid());
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    gcpKmsConfig.setDefault(true);

    String encryptedDek = "encryptedDek";
    String plainTextValue = "value";

    KeyManagementServiceClient keyManagementServiceClient = PowerMockito.mock(KeyManagementServiceClient.class);
    EncryptResponse encryptResponse =
        EncryptResponse.newBuilder()
            .setCiphertext(ByteString.copyFrom(encryptedDek.getBytes(StandardCharsets.ISO_8859_1)))
            .build();
    String resourceName = CryptoKeyName.format(
        gcpKmsConfig.getProjectId(), gcpKmsConfig.getRegion(), gcpKmsConfig.getKeyRing(), gcpKmsConfig.getKeyName());
    doReturn(keyManagementServiceClient).when(gcpKmsEncryptDecryptClient).getClientInternal(any());

    // Encryption Test
    PowerMockito.when(keyManagementServiceClient.encrypt(eq(resourceName), any(ByteString.class)))
        .thenReturn(encryptResponse);
    EncryptedRecord encryptedRecord =
        gcpKmsEncryptDecryptClient.encrypt(plainTextValue, GLOBAL_ACCOUNT_ID, gcpKmsConfig, null);
    assertThat(encryptedRecord).isNotNull();
    assertThat(encryptedRecord.getEncryptionKey()).isEqualTo(encryptedDek);
    assertThat(encryptedRecord.getEncryptionType()).isEqualTo(EncryptionType.GCP_KMS);
    assertThat(encryptedRecord.getKmsId()).isEqualTo(gcpKmsConfig.getUuid());

    ArgumentCaptor<ByteString> captor = ArgumentCaptor.forClass(ByteString.class);
    verify(keyManagementServiceClient, times(1)).encrypt(eq(resourceName), captor.capture());
    ByteString plainTextDek = captor.getValue();

    // Decryption Test
    EncryptedData encryptedData = (EncryptedData) encryptedRecord;
    encryptedData.setUuid(UUIDGenerator.generateUuid());
    DecryptResponse decryptResponse = DecryptResponse.newBuilder().setPlaintext(plainTextDek).build();
    PowerMockito.when(keyManagementServiceClient.decrypt(eq(resourceName), any())).thenReturn(decryptResponse);
    char[] decryptedValue = gcpKmsEncryptDecryptClient.decrypt(encryptedData, gcpKmsConfig);
    assertThat(String.valueOf(decryptedValue)).isEqualTo(plainTextValue);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncrypt_ShouldFail1() {
    String plainTextValue = "value";

    EncryptedRecord encryptedRecord = null;
    try {
      encryptedRecord = gcpKmsEncryptDecryptClient.encrypt(plainTextValue, GLOBAL_ACCOUNT_ID, null, null);
    } catch (SecretManagementDelegateException e) {
      assertThat(e).isNotNull();
    }
    assertThat(encryptedRecord).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncrypt_ShouldFail2() {
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();
    gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setUuid(UUIDGenerator.generateUuid());
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    gcpKmsConfig.setDefault(true);

    String plainTextValue = "value";

    KeyManagementServiceClient keyManagementServiceClient = PowerMockito.mock(KeyManagementServiceClient.class);
    String resourceName = CryptoKeyName.format(
        gcpKmsConfig.getProjectId(), gcpKmsConfig.getRegion(), gcpKmsConfig.getKeyRing(), gcpKmsConfig.getKeyName());
    doReturn(keyManagementServiceClient).when(gcpKmsEncryptDecryptClient).getClientInternal(any());

    // Encryption Test
    PowerMockito.when(keyManagementServiceClient.encrypt(eq(resourceName), any(ByteString.class)))
        .thenThrow(new RuntimeException());
    EncryptedRecord encryptedRecord = null;
    try {
      encryptedRecord = gcpKmsEncryptDecryptClient.encrypt(plainTextValue, GLOBAL_ACCOUNT_ID, gcpKmsConfig, null);
    } catch (DelegateRetryableException e) {
      assertThat(e).isNotNull();
    }
    assertThat(encryptedRecord).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecrypt_ShouldFail1() {
    EncryptedData encryptedData = mock(EncryptedData.class);
    when(encryptedData.getEncryptedValue()).thenReturn(null);
    char[] plainText = gcpKmsEncryptDecryptClient.decrypt(encryptedData, gcpKmsConfig);
    assertThat(plainText).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecrypt_ShouldFail2() {
    EncryptedData encryptedData = mock(EncryptedData.class);
    when(encryptedData.getEncryptedValue()).thenReturn("encryptedValue".toCharArray());
    char[] plainText = null;
    try {
      plainText = gcpKmsEncryptDecryptClient.decrypt(encryptedData, null);
    } catch (SecretManagementDelegateException e) {
      assertThat(e).isNotNull();
    }
    assertThat(plainText).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecrypt_ShoudFail3() {
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();
    gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setUuid(UUIDGenerator.generateUuid());
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    gcpKmsConfig.setDefault(true);

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();
    encryptedData.setUuid(UUIDGenerator.generateUuid());

    KeyManagementServiceClient keyManagementServiceClient = PowerMockito.mock(KeyManagementServiceClient.class);
    doReturn(keyManagementServiceClient).when(gcpKmsEncryptDecryptClient).getClientInternal(any());
    String resourceName = CryptoKeyName.format(
        gcpKmsConfig.getProjectId(), gcpKmsConfig.getRegion(), gcpKmsConfig.getKeyRing(), gcpKmsConfig.getKeyName());
    PowerMockito.when(keyManagementServiceClient.decrypt(eq(resourceName), any())).thenThrow(new RuntimeException());
    char[] decryptedValue = null;
    try {
      decryptedValue = gcpKmsEncryptDecryptClient.decrypt(encryptedData, gcpKmsConfig);
    } catch (DelegateRetryableException e) {
      assertThat(e).isNotNull();
    }
    assertThat(decryptedValue).isNullOrEmpty();
  }
}
