package io.harness.delegate.task.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.ServiceNowException;
import io.harness.jira.ServiceNowActionNG;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.helpers.ext.servicenow.ServiceNowRestClient;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

@OwnedBy(CDC)
@RunWith(PowerMockRunner.class)
@PrepareForTest({Retrofit.class, ServiceNowTaskNgHelper.class})
@PowerMockIgnore({"javax.net.ssl.*"})
public class ServiceNowTaskNGHelperTest extends CategoryTest {
  @Mock private SecretDecryptionService secretDecryptionService;
  @InjectMocks private ServiceNowTaskNgHelper serviceNowTaskNgHelper;

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldValidateCredentials() throws Exception {
    ServiceNowRestClient serviceNowRestClient = Mockito.mock(ServiceNowRestClient.class);
    Retrofit retrofit = Mockito.mock(Retrofit.class);
    Call mockCall = Mockito.mock(Call.class);
    when(serviceNowRestClient.validateConnection(anyString())).thenReturn(mockCall);
    Response<JsonNode> jsonNodeResponse = Response.success(null);
    when(mockCall.execute()).thenReturn(jsonNodeResponse);
    PowerMockito.whenNew(Retrofit.class).withAnyArguments().thenReturn(retrofit);
    PowerMockito.when(retrofit.create(ServiceNowRestClient.class)).thenReturn(serviceNowRestClient);

    ServiceNowConnectorDTO serviceNowConnectorDTO =
        ServiceNowConnectorDTO.builder()
            .serviceNowUrl("https://harness.service-now.com/")
            .username("username")
            .passwordRef(SecretRefData.builder().decryptedValue(new char[] {'3', '4', 'f', '5', '1'}).build())
            .build();
    ServiceNowTaskNGResponse response =
        serviceNowTaskNgHelper.getServiceNowResponse(ServiceNowTaskNGParameters.builder()
                                                         .action(ServiceNowActionNG.VALIDATE_CREDENTIALS)
                                                         .serviceNowConnectorDTO(serviceNowConnectorDTO)
                                                         .build());
    assertThat(response.getDelegateMetaInfo()).isNull();
    verify(secretDecryptionService).decrypt(any(), any());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldValidateCredentialsFailure() throws Exception {
    ServiceNowRestClient serviceNowRestClient = Mockito.mock(ServiceNowRestClient.class);
    Retrofit retrofit = Mockito.mock(Retrofit.class);
    Call mockCall = Mockito.mock(Call.class);
    when(serviceNowRestClient.validateConnection(anyString())).thenReturn(mockCall);
    Response<JsonNode> jsonNodeResponse = Response.error(401, ResponseBody.create(MediaType.parse("JSON"), ""));
    when(mockCall.execute()).thenReturn(jsonNodeResponse);
    PowerMockito.whenNew(Retrofit.class).withAnyArguments().thenReturn(retrofit);
    PowerMockito.when(retrofit.create(ServiceNowRestClient.class)).thenReturn(serviceNowRestClient);

    ServiceNowConnectorDTO serviceNowConnectorDTO =
        ServiceNowConnectorDTO.builder()
            .serviceNowUrl("https://harness.service-now.com/")
            .username("username")
            .passwordRef(SecretRefData.builder().decryptedValue(new char[] {'3', '4', 'f', '5', '1'}).build())
            .build();
    assertThatThrownBy(
        ()
            -> serviceNowTaskNgHelper.getServiceNowResponse(ServiceNowTaskNGParameters.builder()
                                                                .action(ServiceNowActionNG.VALIDATE_CREDENTIALS)
                                                                .serviceNowConnectorDTO(serviceNowConnectorDTO)
                                                                .build()))
        .isInstanceOf(ServiceNowException.class)
        .hasMessage("ServiceNowException: Invalid ServiceNow credentials");
    verify(secretDecryptionService).decrypt(any(), any());
  }
}
