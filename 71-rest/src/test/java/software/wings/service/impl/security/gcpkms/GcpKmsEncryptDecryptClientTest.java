package software.wings.service.impl.security.gcpkms;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.protobuf.ByteString;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.OwnerRule.Owner;
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

import java.nio.charset.StandardCharsets;

@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest({GcpKmsEncryptDecryptClient.class, KeyManagementServiceClient.class})
@PowerMockIgnore({"org.apache.http.conn.ssl.", "javax.net.ssl.", "javax.crypto.*"})
public class GcpKmsEncryptDecryptClientTest {
  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient;
  private GcpKmsConfig gcpKmsConfig;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptDecrypt() {
    gcpKmsEncryptDecryptClient = spy(new GcpKmsEncryptDecryptClient(timeLimiter));
    gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", "credentials".toCharArray());
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
    doReturn(keyManagementServiceClient).when(gcpKmsEncryptDecryptClient).getClient(gcpKmsConfig);

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
}
