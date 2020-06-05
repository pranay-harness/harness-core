package software.wings.service.impl.security;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.KeyVaultErrorException;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.CommandExecutionException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecord;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.helpers.ext.cyberark.CyberArkRestClient;
import software.wings.helpers.ext.cyberark.CyberArkRestClientFactory;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.impl.security.cyberark.CyberArkReadResponse;
import software.wings.service.impl.security.gcpkms.GcpKmsEncryptDecryptClient;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.intfc.security.CustomSecretsManagerDelegateService;

import java.io.IOException;

/**
 * @author marklu on 2019-03-06
 */
@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyVaultADALAuthenticator.class, KeyVaultClient.class, CyberArkRestClientFactory.class})
@PowerMockIgnore(
    {"okhttp3.*", "javax.security.*", "org.apache.http.conn.ssl.", "javax.net.ssl.", "javax.crypto.*", "sun.*"})
public class SecretManagementDelegateServiceTest extends CategoryTest {
  private SecretManagementDelegateServiceImpl secretManagementDelegateService;
  private TimeLimiter timeLimiter = new SimpleTimeLimiter();
  @Mock private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  @Mock private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient;
  @Mock private CustomSecretsManagerDelegateService customSecretsManagerDelegateService;

  @Before
  public void setup() throws Exception {
    initMocks(this);
    secretManagementDelegateService = spy(new SecretManagementDelegateServiceImpl(
        timeLimiter, kmsEncryptDecryptClient, gcpKmsEncryptDecryptClient, customSecretsManagerDelegateService));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCyberArkConfigValidation_shouldPass() throws IOException {
    CyberArkConfig cyberArkConfig = getCyberArkConfig("/");
    mockStatic(CyberArkRestClientFactory.class);
    CyberArkRestClient cyberArkRestClient = mock(CyberArkRestClient.class);
    CyberArkReadResponse cyberArkReadResponse = mock(CyberArkReadResponse.class);
    Call<CyberArkReadResponse> cyberArkReadResponseCall = mock(Call.class);
    Response<CyberArkReadResponse> cyberArkReadResponseResponse = Response.success(cyberArkReadResponse);
    when(CyberArkRestClientFactory.create(cyberArkConfig)).thenReturn(cyberArkRestClient);
    when(cyberArkRestClient.readSecret(anyString(), anyString())).thenReturn(cyberArkReadResponseCall);
    when(cyberArkReadResponseCall.execute()).thenReturn(cyberArkReadResponseResponse);
    secretManagementDelegateService.validateCyberArkConfig(cyberArkConfig);
  }

  @Test(expected = SecretManagementDelegateException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCyberArkConfigValidation_shouldFail() throws IOException {
    CyberArkConfig cyberArkConfig = getCyberArkConfig("/");
    mockStatic(CyberArkRestClientFactory.class);
    CyberArkRestClient cyberArkRestClient = mock(CyberArkRestClient.class);
    Call<CyberArkReadResponse> cyberArkReadResponseCall = mock(Call.class);
    Response<CyberArkReadResponse> cyberArkReadResponseResponse =
        Response.error(ResponseBody.create(MediaType.parse("application/json"), "error"),
            new okhttp3.Response.Builder()
                .message("xyz")
                .code(500)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .build());
    when(CyberArkRestClientFactory.create(cyberArkConfig)).thenReturn(cyberArkRestClient);
    when(cyberArkRestClient.readSecret(anyString(), anyString())).thenReturn(cyberArkReadResponseCall);
    when(cyberArkReadResponseCall.execute()).thenReturn(cyberArkReadResponseResponse);
    secretManagementDelegateService.validateCyberArkConfig(cyberArkConfig);
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

  @Test(expected = SecretManagementDelegateException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_azureVault_decrypt_shouldFail() {
    EncryptedRecord encryptedRecord = mock(EncryptedRecord.class);
    AzureVaultConfig azureVaultConfig = mock(AzureVaultConfig.class);
    KeyVaultClient keyVaultClient = PowerMockito.mock(KeyVaultClient.class);
    mockStatic(KeyVaultADALAuthenticator.class);
    when(encryptedRecord.getPath()).thenReturn("secret");
    when(encryptedRecord.getEncryptedValue()).thenReturn("value".toCharArray());
    when(azureVaultConfig.getClientId()).thenReturn("clientId");
    when(azureVaultConfig.getSecretKey()).thenReturn("secretKey");
    when(azureVaultConfig.getEncryptionServiceUrl()).thenReturn("encryptionServiceUrl");
    when(KeyVaultADALAuthenticator.getClient(azureVaultConfig.getClientId(), azureVaultConfig.getSecretKey()))
        .thenReturn(keyVaultClient);
    when(keyVaultClient.getSecret(any(), any(), any())).thenThrow(KeyVaultErrorException.class);
    secretManagementDelegateService.decrypt(encryptedRecord, azureVaultConfig);
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