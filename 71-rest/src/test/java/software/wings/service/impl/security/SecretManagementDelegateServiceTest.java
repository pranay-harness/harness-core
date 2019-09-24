package software.wings.service.impl.security;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.security.encryption.EncryptedRecord;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;

/**
 * @author marklu on 2019-03-06
 */
@Slf4j
public class SecretManagementDelegateServiceTest extends CategoryTest {
  private SecretManagementDelegateServiceImpl secretManagementDelegateService;
  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  private MockWebServer mockWebServer = new MockWebServer();

  private ScmSecret scmSecret = new ScmSecret();
  private String accountId = UUIDGenerator.generateUuid();
  private KmsConfig kmsConfig;

  @Before
  public void setup() throws Exception {
    initMocks(this);

    mockWebServer.start();

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

  @After
  public void shutdown() throws Exception {
    mockWebServer.shutdown();
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
      assertThat(new String(decryptedSecret)).isEqualTo(secret);
    }

    assertThat(secretManagementDelegateService.getKmsEncryptionKeyCacheSize()).isEqualTo(1);
  }

  @Test
  @Category(UnitTests.class)
  public void testCyberArkConfigValidation() {
    String url = mockWebServer.url("/").url().toString();
    CyberArkConfig cyberArkConfig = getCyberArkConfig(url);

    // 404 http status code is expected and should succeed the validation
    MockResponse mockResponse = new MockResponse().setResponseCode(404);
    mockWebServer.enqueue(mockResponse);
    secretManagementDelegateService.validateCyberArkConfig(cyberArkConfig);

    try {
      mockResponse = new MockResponse().setResponseCode(500);
      mockWebServer.enqueue(mockResponse);
      secretManagementDelegateService.validateCyberArkConfig(cyberArkConfig);
      fail("Exception is expected");
    } catch (SecretManagementDelegateException e) {
      // Exception is expected.
    }
  }

  private CyberArkConfig getCyberArkConfig(String url) {
    final CyberArkConfig cyberArkConfig = new CyberArkConfig();
    cyberArkConfig.setName("TestCyberArk");
    cyberArkConfig.setDefault(true);
    cyberArkConfig.setCyberArkUrl(url);
    cyberArkConfig.setAppId(generateUuid());
    return cyberArkConfig;
  }
}