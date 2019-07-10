package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.MARK;
import static org.junit.Assert.assertEquals;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.security.encryption.EncryptedRecord;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;

/**
 * @author marklu on 2019-03-06
 */
public class SecretManagementDelegateServiceTest extends CategoryTest {
  private SecretManagementDelegateServiceImpl secretManagementDelegateService;
  private TimeLimiter timeLimiter = new SimpleTimeLimiter();

  private ScmSecret scmSecret = new ScmSecret();
  private String accountId = UUIDGenerator.generateUuid();
  private KmsConfig kmsConfig;

  @Before
  public void setup() {
    secretManagementDelegateService = new SecretManagementDelegateServiceImpl(timeLimiter);

    kmsConfig = KmsConfig.builder()
                    .name("TestAwsKMS")
                    .accessKey("AKIAJXKK6OAOHQ5MO34Q")
                    .kmsArn("arn:aws:kms:us-east-1:448640225317:key/4feb7890-a727-4f88-af43-378b5a88e77c")
                    .secretKey(scmSecret.decryptToString(new SecretName("kms_qa_secret_key")))
                    .region("us-east-1")
                    .build();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setDefault(true);
  }

  @Test
  @Owner(emails = MARK)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void test_EncryptDecryptKmsSecret_ShouldSucceed() {
    String secret = "TopSecret";
    EncryptedRecord encryptedRecord =
        secretManagementDelegateService.encrypt(accountId, secret.toCharArray(), kmsConfig);
    ((EncryptedData) encryptedRecord).setUuid(UUIDGenerator.generateUuid());
    for (int i = 0; i < 10; i++) {
      char[] decryptedSecret = secretManagementDelegateService.decrypt(encryptedRecord, kmsConfig);
      assertEquals(secret, new String(decryptedSecret));
    }

    assertEquals(1, secretManagementDelegateService.getKmsEncryptionKeyCacheSize());
  }
}