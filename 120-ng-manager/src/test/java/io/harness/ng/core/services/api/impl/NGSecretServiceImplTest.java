package io.harness.ng.core.services.api.impl;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;

import java.io.IOException;

public class NGSecretServiceImplTest extends BaseTest {
  private SecretManagerClient secretManagerClient;
  private NGSecretServiceImpl ngSecretService;
  private final String SECRET_ID = "SECRET_ID";
  private final String SECRET_NAME = "SECRET_NAME";
  private final String ACCOUNT_ID = "ACCOUNT_ID";

  @Before
  public void doSetup() {
    secretManagerClient = mock(SecretManagerClient.class);
    ngSecretService = new NGSecretServiceImpl(secretManagerClient);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecretById() throws IOException {
    EncryptedData encryptedData = EncryptedData.builder().name(SECRET_NAME).build();
    RestResponse<EncryptedData> restResponse = new RestResponse<>(encryptedData);
    Response<RestResponse<EncryptedData>> response = Response.success(restResponse);
    Call<RestResponse<EncryptedData>> restResponseCall = (Call<RestResponse<EncryptedData>>) mock(Call.class);

    when(secretManagerClient.getSecretById(any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    EncryptedData returnedEncryptedData = ngSecretService.getSecretById(ACCOUNT_ID, SECRET_ID);
    assertThat(returnedEncryptedData).isNotNull();
    assertThat(returnedEncryptedData.getName()).isEqualTo(SECRET_NAME);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecretById_For_FailedCall() throws IOException {
    Response<RestResponse<EncryptedData>> response = Response.error(HttpStatus.SC_BAD_GATEWAY, new ResponseBody() {
      @Override
      public MediaType contentType() {
        return null;
      }

      @Override
      public long contentLength() {
        return 0;
      }

      @Override
      public BufferedSource source() {
        return null;
      }
    });

    Call<RestResponse<EncryptedData>> restResponseCall = (Call<RestResponse<EncryptedData>>) mock(Call.class);

    when(secretManagerClient.getSecretById(anyString(), anyString(), anyString())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    boolean exceptionThrown = false;
    try {
      ngSecretService.getSecretById(ACCOUNT_ID, SECRET_ID);
    } catch (SecretManagementException sme) {
      exceptionThrown = true;
      assertThat(sme.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecretById_For_Exception() throws IOException {
    NGSecretManagerServiceImpl ngSecretManager = new NGSecretManagerServiceImpl(secretManagerClient);
    Call<RestResponse<EncryptedData>> restResponseCall = (Call<RestResponse<EncryptedData>>) mock(Call.class);
    when(secretManagerClient.getSecretById(any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());

    boolean exceptionThrown = false;
    try {
      ngSecretService.getSecretById(ACCOUNT_ID, SECRET_ID);
    } catch (SecretManagementException sme) {
      exceptionThrown = true;
      assertThat(sme.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }
}
