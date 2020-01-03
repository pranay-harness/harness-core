package software.wings.service.impl.security.kms;

import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * @author rktummala
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(KmsEncryptDecryptClient.class)
public class KmsEncryptDecryptClientTest extends WingsBaseTest {
  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  private KmsConfig kmsConfig;

  @Test
  @Owner(developers = RAMA)
  @Repeat(times = 3, successes = 1)
  @Category(UnitTests.class)
  public void testDecryptionFallbackToDelegate()
      throws IllegalAccessException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException,
             NoSuchAlgorithmException, NoSuchPaddingException {
    kmsEncryptDecryptClient = spy(new KmsEncryptDecryptClient(timeLimiter));
    kmsConfig = KmsConfig.builder()
                    .name("TestAwsKMS")
                    .accessKey("access_key")
                    .secretKey("secret_key")
                    .kmsArn("kms_arn")
                    .region("us-east-1")
                    .build();
    kmsConfig.setAccountId("__GLOBAL_APP_ID__");
    kmsConfig.setDefault(true);

    AWSKMS awskms = mock(AWSKMS.class);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);
    FieldUtils.writeField(byteBuffer, "hb", "abcd".getBytes(), true);
    DecryptResult decryptResult = mock(DecryptResult.class);
    FieldUtils.writeField(decryptResult, "plaintext", byteBuffer, true);
    doReturn(byteBuffer).when(decryptResult).getPlaintext();
    doReturn(decryptResult).when(awskms).decrypt(any(DecryptRequest.class));
    doReturn(awskms).when(kmsEncryptDecryptClient).getKmsClient(kmsConfig);
    byte[] data = "abc".getBytes();
    doReturn(data).when(kmsEncryptDecryptClient).decryptPlainTextKey(any(), any());

    PowerMockito.mockStatic(KmsEncryptDecryptClient.class);
    PowerMockito.when(KmsEncryptDecryptClient.decrypt(any(char[].class), any(Key.class))).thenReturn("abcd");
    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(ACCOUNT_ID)
                                      .kmsId("kms_id")
                                      .encryptionType(EncryptionType.KMS)
                                      .encryptedValue(new char[] {'a', 'b'})
                                      .encryptionKey("key1")
                                      .build();
    encryptedData.setUuid("id1");
    EncryptedRecordData encryptedRecordData =
        kmsEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(encryptedData, kmsConfig);
    assertThat(encryptedRecordData.getEncryptionType()).isEqualTo(EncryptionType.KMS);
  }
}
