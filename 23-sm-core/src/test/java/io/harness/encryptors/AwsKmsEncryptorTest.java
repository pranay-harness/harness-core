package io.harness.encryptors;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.encryptors.clients.AwsKmsEncryptor;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;

import software.wings.beans.KmsConfig;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;
import com.amazonaws.services.kms.model.KeyUnavailableException;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AwsKmsEncryptorTest extends CategoryTest {
  private final SecureRandom secureRandom = new SecureRandom();
  private AwsKmsEncryptor awsKmsEncryptor;
  private KmsConfig kmsConfig;
  private AWSKMS awskms;

  @Before
  public void setup() {
    TimeLimiter timeLimiter = new SimpleTimeLimiter();
    awsKmsEncryptor = spy(new AwsKmsEncryptor(timeLimiter));
    kmsConfig = KmsConfig.builder()
                    .uuid(UUIDGenerator.generateUuid())
                    .name(UUIDGenerator.generateUuid())
                    .encryptionType(EncryptionType.KMS)
                    .accountId(UUIDGenerator.generateUuid())
                    .region("us-east-1")
                    .accessKey(UUIDGenerator.generateUuid())
                    .kmsArn(UUIDGenerator.generateUuid())
                    .secretKey(UUIDGenerator.generateUuid())
                    .isDefault(false)
                    .build();
    awskms = mock(AWSKMS.class);
    when(awsKmsEncryptor.getKmsClient(kmsConfig)).thenReturn(awskms);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptDecryptSecret_withRetry() {
    String value = UUIDGenerator.generateUuid();
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);

    GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
    dataKeyRequest.setKeyId(kmsConfig.getKmsArn());
    dataKeyRequest.setKeySpec("AES_128");
    GenerateDataKeyResult generateDataKeyResult = mock(GenerateDataKeyResult.class);
    when(generateDataKeyResult.getPlaintext()).thenReturn(ByteBuffer.wrap(bytes));
    when(generateDataKeyResult.getCiphertextBlob()).thenReturn(ByteBuffer.wrap(bytes));
    when(awskms.generateDataKey(dataKeyRequest))
        .thenThrow(new KeyUnavailableException("Dummy error"))
        .thenReturn(generateDataKeyResult);

    EncryptedRecord encryptedRecord = awsKmsEncryptor.encryptSecret(UUIDGenerator.generateUuid(), value, kmsConfig);
    assertThat(encryptedRecord).isNotNull();
    assertThat(encryptedRecord.getEncryptionKey()).isEqualTo(new String(bytes, StandardCharsets.ISO_8859_1));
    EncryptedRecordData testRecord = EncryptedRecordData.builder()
                                         .uuid(UUIDGenerator.generateUuid())
                                         .encryptionKey(encryptedRecord.getEncryptionKey())
                                         .encryptedValue(encryptedRecord.getEncryptedValue())
                                         .build();
    DecryptRequest decryptRequest =
        new DecryptRequest().withCiphertextBlob(StandardCharsets.ISO_8859_1.encode(testRecord.getEncryptionKey()));
    DecryptResult decryptResult = mock(DecryptResult.class);
    when(decryptResult.getPlaintext()).thenReturn(ByteBuffer.wrap(bytes));
    when(awskms.decrypt(decryptRequest))
        .thenThrow(new KeyUnavailableException("Dummy error"))
        .thenReturn(decryptResult);
    char[] returnedValue = awsKmsEncryptor.fetchSecretValue(UUIDGenerator.generateUuid(), testRecord, kmsConfig);
    assertThat(returnedValue).isEqualTo(value.toCharArray());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptSecret_shouldThrowException() {
    String value = UUIDGenerator.generateUuid();
    when(awskms.generateDataKey(any())).thenThrow(new KeyUnavailableException("Dummy error"));
    try {
      awsKmsEncryptor.encryptSecret(UUIDGenerator.generateUuid(), value, kmsConfig);
      fail("The test method should have thrown an exception");
    } catch (DelegateRetryableException e) {
      assertThat(e.getCause().getMessage()).isEqualTo("Encryption failed after 3 retries");
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testFetchSecretValue_shouldThrowException() {
    EncryptedRecordData testRecord = EncryptedRecordData.builder()
                                         .uuid(UUIDGenerator.generateUuid())
                                         .name(UUIDGenerator.generateUuid())
                                         .encryptionKey(UUIDGenerator.generateUuid())
                                         .encryptedValue(UUIDGenerator.generateUuid().toCharArray())
                                         .build();
    when(awskms.decrypt(any())).thenThrow(new KeyUnavailableException("Dummy error"));
    try {
      awsKmsEncryptor.fetchSecretValue(UUIDGenerator.generateUuid(), testRecord, kmsConfig);
      fail("The test method should have thrown an exception");
    } catch (DelegateRetryableException e) {
      assertThat(e.getCause().getMessage())
          .isEqualTo(String.format("Decryption failed for encryptedData %s after 3 retries", testRecord.getName()));
    }
  }
}
