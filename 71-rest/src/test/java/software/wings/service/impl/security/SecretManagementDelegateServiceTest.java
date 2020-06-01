package software.wings.service.impl.security;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.CommandExecutionException;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.beans.CyberArkConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.impl.security.gcpkms.GcpKmsEncryptDecryptClient;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.intfc.security.CustomSecretsManagerDelegateService;

/**
 * @author marklu on 2019-03-06
 */
@Slf4j
public class SecretManagementDelegateServiceTest extends CategoryTest {
  private SecretManagementDelegateServiceImpl secretManagementDelegateService;
  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  @Mock private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  @Mock private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient;
  @Mock private CustomSecretsManagerDelegateService customSecretsManagerDelegateService;
  private MockWebServer mockWebServer = new MockWebServer();

  @Before
  public void setup() throws Exception {
    initMocks(this);

    mockWebServer.start();

    secretManagementDelegateService = new SecretManagementDelegateServiceImpl(
        timeLimiter, kmsEncryptDecryptClient, gcpKmsEncryptDecryptClient, customSecretsManagerDelegateService);
  }

  @After
  public void shutdown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @Owner(developers = UTKARSH)
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

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_customSecretsManagerConfig_decrypt_shouldSucceed() {
    EncryptedData data = mock(EncryptedData.class);
    CustomSecretsManagerConfig config = mock(CustomSecretsManagerConfig.class);
    when(customSecretsManagerDelegateService.fetchSecret(data, config)).thenReturn("value".toCharArray());
    char[] secret = secretManagementDelegateService.decrypt(data, config);
    assertThat(secret).isEqualTo("value".toCharArray());
  }

  @Test(expected = SecretManagementDelegateException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_customSecretsManagerConfig_decrypt_shouldFailWithRetry() {
    EncryptedData data = mock(EncryptedData.class);
    CustomSecretsManagerConfig config = mock(CustomSecretsManagerConfig.class);
    when(customSecretsManagerDelegateService.fetchSecret(data, config)).thenThrow(CommandExecutionException.class);
    secretManagementDelegateService.decrypt(data, config);
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