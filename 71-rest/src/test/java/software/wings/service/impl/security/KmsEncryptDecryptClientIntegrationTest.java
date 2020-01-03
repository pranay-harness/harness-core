package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import io.harness.CategoryTest;
import io.harness.category.element.IntegrationTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.security.encryption.EncryptedRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;

/**
 * @author marklu on 10/7/19
 */
public class KmsEncryptDecryptClientIntegrationTest extends CategoryTest {
  private static final String QA_KMS_REGION = "us-east-1";

  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  private KmsEncryptDecryptClient kmsEncryptDecryptClient;

  private ScmSecret scmSecret = new ScmSecret();
  private String accountId = UUIDGenerator.generateUuid();
  private KmsConfig kmsConfig;

  @Before
  public void setUp() {
    kmsEncryptDecryptClient = new KmsEncryptDecryptClient(timeLimiter);

    kmsConfig = KmsConfig.builder()
                    .name("TestAwsKMS")
                    .accessKey(scmSecret.decryptToString(new SecretName("kms_qa_access_key")))
                    .secretKey(scmSecret.decryptToString(new SecretName("kms_qa_secret_key")))
                    .kmsArn(scmSecret.decryptToString(new SecretName("kms_qa_arn")))
                    .region(QA_KMS_REGION)
                    .build();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setDefault(true);
  }

  @Test
  @Owner(developers = UTKARSH, intermittent = true)
  @Repeat(times = 3, successes = 1)
  @Category(IntegrationTests.class)
  public void test_EncryptDecryptKmsSecret_ShouldSucceed() {
    String secret = "TopSecret";
    EncryptedRecord encryptedRecord = kmsEncryptDecryptClient.encrypt(accountId, secret.toCharArray(), kmsConfig);
    ((EncryptedData) encryptedRecord).setUuid(UUIDGenerator.generateUuid());
    for (int i = 0; i < 5; i++) {
      char[] decryptedSecret = kmsEncryptDecryptClient.decrypt(encryptedRecord, kmsConfig);
      assertThat(new String(decryptedSecret)).isEqualTo(secret);
    }
  }
}
