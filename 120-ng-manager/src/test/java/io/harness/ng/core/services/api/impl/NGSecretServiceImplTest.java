package io.harness.ng.core.services.api.impl;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.impl.security.SecretText;

import java.io.IOException;

public class NGSecretServiceImplTest extends BaseTest {
  private SecretManagerClient secretManagerClient;
  private NGSecretServiceImpl ngSecretService;
  private final String SECRET_ID = "SECRET_ID";
  private final String SECRET_NAME = "SECRET_NAME";
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String UUID = "UUID";

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
    Response<RestResponse<EncryptedData>> response = Response.error(SC_BAD_GATEWAY, new ResponseBody() {
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
  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret() throws IOException {
    String secretEncryptionId = randomAlphabetic(10);
    RestResponse<String> restResponse = new RestResponse<>(secretEncryptionId);
    Response<RestResponse<String>> response = Response.success(restResponse);
    Call<RestResponse<String>> restResponseCall = (Call<RestResponse<String>>) mock(Call.class);
    SecretText randomSecretText = random(SecretText.class);

    when(secretManagerClient.createSecret(eq(ACCOUNT_ID), anyBoolean(), eq(randomSecretText)))
        .thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    String returnedSecretEncryptedId = ngSecretService.createSecret(ACCOUNT_ID, true, randomSecretText);
    assertThat(returnedSecretEncryptedId).isNotNull();
    assertThat(returnedSecretEncryptedId).isEqualTo(secretEncryptionId);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret_For_FailedCall() throws IOException {
    Response<RestResponse<String>> response = Response.error(SC_BAD_GATEWAY, new ResponseBody() {
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
    Call<RestResponse<String>> restResponseCall = (Call<RestResponse<String>>) mock(Call.class);
    SecretText randomSecretText = random(SecretText.class);

    when(secretManagerClient.createSecret(eq(ACCOUNT_ID), anyBoolean(), eq(randomSecretText)))
        .thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    boolean exceptionThrown = false;
    try {
      ngSecretService.createSecret(ACCOUNT_ID, true, randomSecretText);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret_For_Exception() throws IOException {
    Call<RestResponse<String>> restResponseCall = (Call<RestResponse<String>>) mock(Call.class);
    SecretText randomSecretText = random(SecretText.class);

    when(secretManagerClient.createSecret(eq(ACCOUNT_ID), anyBoolean(), eq(randomSecretText)))
        .thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());

    boolean exceptionThrown = false;
    try {
      ngSecretService.createSecret(ACCOUNT_ID, true, randomSecretText);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateSecret() throws IOException {
    RestResponse<Boolean> restResponse = new RestResponse<>(true);
    Response<RestResponse<Boolean>> response = Response.success(restResponse);
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);
    SecretText randomSecretText = random(SecretText.class);

    when(secretManagerClient.updateSecret(ACCOUNT_ID, UUID, randomSecretText)).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    Boolean returnedResult = ngSecretService.updateSecret(ACCOUNT_ID, UUID, randomSecretText);
    assertThat(returnedResult).isNotNull();
    assertThat(returnedResult).isEqualTo(true);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateSecret_For_FailedCall() throws IOException {
    Response<RestResponse<Boolean>> response = Response.error(SC_BAD_GATEWAY, new ResponseBody() {
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
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);
    SecretText randomSecretText = random(SecretText.class);

    when(secretManagerClient.updateSecret(ACCOUNT_ID, UUID, randomSecretText)).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    boolean exceptionThrown = false;
    try {
      ngSecretService.updateSecret(ACCOUNT_ID, UUID, randomSecretText);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateSecret_For_Exception() throws IOException {
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);
    SecretText randomSecretText = random(SecretText.class);

    when(secretManagerClient.updateSecret(ACCOUNT_ID, UUID, randomSecretText)).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());

    boolean exceptionThrown = false;
    try {
      ngSecretService.updateSecret(ACCOUNT_ID, UUID, randomSecretText);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteSecret() throws IOException {
    RestResponse<Boolean> restResponse = new RestResponse<>(true);
    Response<RestResponse<Boolean>> response = Response.success(restResponse);
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);

    when(secretManagerClient.deleteSecret(ACCOUNT_ID, UUID)).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    Boolean returnedResult = ngSecretService.deleteSecret(ACCOUNT_ID, UUID);
    assertThat(returnedResult).isNotNull();
    assertThat(returnedResult).isEqualTo(true);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteSecret_For_FailedCall() throws IOException {
    Response<RestResponse<Boolean>> response = Response.error(SC_BAD_GATEWAY, new ResponseBody() {
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
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);

    when(secretManagerClient.deleteSecret(ACCOUNT_ID, UUID)).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    boolean exceptionThrown = false;
    try {
      ngSecretService.deleteSecret(ACCOUNT_ID, UUID);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteSecret_For_Exception() throws IOException {
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);

    when(secretManagerClient.deleteSecret(ACCOUNT_ID, UUID)).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());

    boolean exceptionThrown = false;
    try {
      ngSecretService.deleteSecret(ACCOUNT_ID, UUID);
    } catch (SecretManagementException ex) {
      exceptionThrown = true;
      assertThat(ex.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
    }
    assertThat(exceptionThrown).isTrue();
  }
}
