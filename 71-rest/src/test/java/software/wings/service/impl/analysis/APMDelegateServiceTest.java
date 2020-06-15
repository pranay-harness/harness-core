package software.wings.service.impl.analysis;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.common.VerificationConstants.AZURE_BASE_URL;
import static software.wings.common.VerificationConstants.AZURE_TOKEN_URL;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.APMVerificationState;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class APMDelegateServiceTest {
  @Mock private RequestExecutor mockRequestExecutor;
  @Mock private EncryptionService mockEncryptionService;
  @Captor private ArgumentCaptor<Call<Object>> argumentCaptor;

  @InjectMocks private APMDelegateService apmDelegateService = new APMDelegateServiceImpl();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(mockEncryptionService.getDecryptedValue(any())).thenReturn("decryptedSecret".toCharArray());

    when(mockRequestExecutor.executeRequest(any(), any())).thenReturn("{\"test\":\"value\"}");
    Map tokenResponse = new HashMap();
    tokenResponse.put("access_token", "accessToken");
    when(mockRequestExecutor.executeRequest(any())).thenReturn(tokenResponse);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testFetch_withSecrets() {
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().fieldName("apiTokenField").build();
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Api-Token ${apiTokenField}");
    APMValidateCollectorConfig validateCollectorConfig = APMValidateCollectorConfig.builder()
                                                             .encryptedDataDetails(Arrays.asList(encryptedDataDetail))
                                                             .baseUrl("http://sampleUrl.com/")
                                                             .url("api/v1/services")
                                                             .headers(headers)
                                                             .collectionMethod(APMVerificationState.Method.GET)
                                                             .build();

    String response = apmDelegateService.fetch(validateCollectorConfig, ThirdPartyApiCallLog.builder().build());
    verify(mockRequestExecutor).executeRequest(any(), argumentCaptor.capture());
    Call<Object> request = argumentCaptor.getValue();
    assertThat(request.request().url().toString()).isEqualTo("http://sampleurl.com/api/v1/services");
    assertThat(request.request().headers().get("Authorization")).isEqualTo("Api-Token decryptedSecret");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testFetch_azureLogAnalytics() throws Exception {
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().fieldName("client_id").build();
    EncryptedDataDetail encryptedDataDetail2 = EncryptedDataDetail.builder().fieldName("client_secret").build();
    EncryptedDataDetail encryptedDataDetail3 = EncryptedDataDetail.builder().fieldName("tenant_id").build();
    when(mockEncryptionService.getDecryptedValue(encryptedDataDetail)).thenReturn("clientId".toCharArray());
    when(mockEncryptionService.getDecryptedValue(encryptedDataDetail2)).thenReturn("clientSecret".toCharArray());
    when(mockEncryptionService.getDecryptedValue(encryptedDataDetail3)).thenReturn("tenantId".toCharArray());

    Map<String, String> headers = new HashMap<>();
    Map<String, String> options = new HashMap<>();
    options.put("client_id", "${client_id}");
    options.put("tenant_id", "${tenant_id}");
    options.put("client_secret", "${client_secret}");
    APMValidateCollectorConfig validateCollectorConfig =
        APMValidateCollectorConfig.builder()
            .encryptedDataDetails(Arrays.asList(encryptedDataDetail, encryptedDataDetail2, encryptedDataDetail3))
            .baseUrl(AZURE_BASE_URL)
            .url("api/v1/services")
            .headers(headers)
            .options(options)
            .collectionMethod(APMVerificationState.Method.POST)
            .build();

    String response = apmDelegateService.fetch(validateCollectorConfig, ThirdPartyApiCallLog.builder().build());

    verify(mockRequestExecutor).executeRequest(argumentCaptor.capture());
    Call<Object> request = argumentCaptor.getValue();
    assertThat(request.request().url().toString()).contains(AZURE_TOKEN_URL);
  }
}
