package io.harness.ng.core.api.impl;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnexpectedException;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.resources.secretsmanagement.EncryptedDataMapper;
import software.wings.security.encryption.EncryptedData;

import java.io.IOException;

public class NGSecretServiceImplTest extends CategoryTest {
  private SecretManagerClient secretManagerClient;
  private NGSecretServiceImpl ngSecretService;
  private NGSecretServiceImpl spyNGSecretService;
  private SecretEntityReferenceHelper secretEntityReferenceHelper;
  private final String SECRET_IDENTIFIER = "SECRET_ID";
  private final String ACCOUNT_IDENTIFIER = "ACCOUNT";
  private final String PROJECT_IDENTIFIER = "PROJECT";
  private final String ORG_IDENTIFIER = "ORG";
  private final String IDENTIFIER = "UUID";

  @Before
  public void doSetup() {
    secretManagerClient = mock(SecretManagerClient.class);
    secretEntityReferenceHelper = mock(SecretEntityReferenceHelper.class);
    ngSecretService = new NGSecretServiceImpl(secretManagerClient, secretEntityReferenceHelper);
    spyNGSecretService = spy(ngSecretService);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecretById() throws IOException {
    EncryptedDataDTO encryptedData = random(EncryptedDataDTO.class);
    RestResponse<EncryptedDataDTO> restResponse = new RestResponse<>(encryptedData);
    Response<RestResponse<EncryptedDataDTO>> response = Response.success(restResponse);
    Call<RestResponse<EncryptedDataDTO>> restResponseCall = mock(Call.class);

    when(secretManagerClient.getSecret(any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    EncryptedData returnedEncryptedData =
        ngSecretService.get(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SECRET_IDENTIFIER);
    assertThat(returnedEncryptedData).isNotNull();
    assertThat(returnedEncryptedData.getName()).isEqualTo(encryptedData.getName());
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecretById_For_FailedCall() throws IOException {
    Response<RestResponse<EncryptedDataDTO>> response = Response.error(SC_BAD_GATEWAY, new ResponseBody() {
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

    Call<RestResponse<EncryptedDataDTO>> restResponseCall = mock(Call.class);

    when(secretManagerClient.getSecret(any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    try {
      ngSecretService.get(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SECRET_IDENTIFIER);
      fail("Execution should not reach here");
    } catch (Exception exception) {
      // not required
    }
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetSecretById_For_Exception() throws IOException {
    Call<RestResponse<EncryptedDataDTO>> restResponseCall = mock(Call.class);
    when(secretManagerClient.getSecret(any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());

    try {
      ngSecretService.get(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SECRET_IDENTIFIER);
      fail("Execution should not reach here");
    } catch (Exception exception) {
      // not required
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret() throws IOException {
    EncryptedDataDTO dto = random(EncryptedDataDTO.class);
    RestResponse<EncryptedDataDTO> restResponse = new RestResponse<>(dto);
    Response<RestResponse<EncryptedDataDTO>> response = Response.success(restResponse);
    Call<RestResponse<EncryptedDataDTO>> restResponseCall = mock(Call.class);
    SecretTextDTO randomSecretText = random(SecretTextDTO.class);

    when(secretManagerClient.createSecret(any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    EncryptedData savedData = ngSecretService.create(randomSecretText, false);
    assertThat(savedData).isNotNull();
    assertThat(savedData.getName()).isEqualTo(dto.getName());
    verify(secretEntityReferenceHelper, times(1)).createEntityReferenceForSecret(any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret_For_FailedCall() throws IOException {
    Response<RestResponse<EncryptedDataDTO>> response = Response.error(SC_BAD_GATEWAY, new ResponseBody() {
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
    Call<RestResponse<EncryptedDataDTO>> restResponseCall = (Call<RestResponse<EncryptedDataDTO>>) mock(Call.class);
    SecretTextDTO randomSecretText = random(SecretTextDTO.class);

    when(secretManagerClient.createSecret(any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    try {
      ngSecretService.create(randomSecretText, false);
      fail("Execution should not reach here");
    } catch (Exception ex) {
      // not required
    }
    verify(secretEntityReferenceHelper, times(0)).createEntityReferenceForSecret(any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateSecret_For_Exception() throws IOException {
    Call<RestResponse<EncryptedDataDTO>> restResponseCall = (Call<RestResponse<EncryptedDataDTO>>) mock(Call.class);
    SecretTextDTO randomSecretText = random(SecretTextDTO.class);

    when(secretManagerClient.createSecret(any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());

    try {
      ngSecretService.create(randomSecretText, false);
      fail("Execution should not reach here");
    } catch (UnexpectedException ex) {
      // not required
    }
    verify(secretEntityReferenceHelper, times(0)).createEntityReferenceForSecret(any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateSecret() throws IOException {
    RestResponse<Boolean> restResponse = new RestResponse<>(true);
    Response<RestResponse<Boolean>> response = Response.success(restResponse);
    Call<RestResponse<Boolean>> updateResponseCall = mock(Call.class);

    SecretTextDTO dto = random(SecretTextDTO.class);
    EncryptedData encryptedData = EncryptedDataMapper.fromDTO(dto);

    when(secretManagerClient.updateSecret(any(), any(), any(), any(), any())).thenReturn(updateResponseCall);
    doReturn(encryptedData).when(spyNGSecretService).get(any(), any(), any(), any());

    when(updateResponseCall.execute()).thenReturn(response);

    Boolean returnedResult = spyNGSecretService.update(dto, false);
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
    SecretTextDTO dto = random(SecretTextDTO.class);

    when(secretManagerClient.updateSecret(any(), any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);
    doReturn(EncryptedDataMapper.fromDTO(dto)).when(spyNGSecretService).get(any(), any(), any(), any());

    try {
      spyNGSecretService.update(dto, false);
      fail("Execution should not reach here");
    } catch (Exception ex) {
      // not required
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateSecret_For_Exception() throws IOException {
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);
    SecretTextDTO dto = random(SecretTextDTO.class);

    when(secretManagerClient.updateSecret(any(), any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());
    doReturn(EncryptedDataMapper.fromDTO(dto)).when(spyNGSecretService).get(any(), any(), any(), any());

    try {
      spyNGSecretService.update(dto, false);
      fail("Execution should not reach here");
    } catch (Exception ex) {
      // not required
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteSecret() throws IOException {
    EncryptedDataDTO encryptedData = random(EncryptedDataDTO.class);
    RestResponse<EncryptedDataDTO> restResponse = new RestResponse<>(encryptedData);
    Response<RestResponse<EncryptedDataDTO>> response = Response.success(restResponse);
    Call<RestResponse<EncryptedDataDTO>> restResponseCall = mock(Call.class);
    when(secretManagerClient.getSecret(any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    RestResponse<Boolean> restResponseDeleteCall = new RestResponse<>(true);
    Response<RestResponse<Boolean>> responseDeleteCall = Response.success(restResponseDeleteCall);
    Call<RestResponse<Boolean>> restResponseDeleteCallMock = (Call<RestResponse<Boolean>>) mock(Call.class);
    when(secretManagerClient.deleteSecret(any(), any(), any(), any())).thenReturn(restResponseDeleteCallMock);
    when(restResponseDeleteCallMock.execute()).thenReturn(responseDeleteCall);

    doNothing().when(secretEntityReferenceHelper).deleteSecretEntityReferenceWhenSecretGetsDeleted(any());

    Boolean returnedResult = ngSecretService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
    assertThat(returnedResult).isNotNull();
    assertThat(returnedResult).isEqualTo(true);
    verify(secretEntityReferenceHelper, times(1)).deleteSecretEntityReferenceWhenSecretGetsDeleted(any());
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

    when(secretManagerClient.deleteSecret(any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenReturn(response);

    try {
      ngSecretService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
      fail("Execution should not reach here.");
    } catch (Exception ex) {
      // not required
    }
    verify(secretEntityReferenceHelper, times(0)).deleteSecretEntityReferenceWhenSecretGetsDeleted(any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteSecret_For_Exception() throws IOException {
    Call<RestResponse<Boolean>> restResponseCall = (Call<RestResponse<Boolean>>) mock(Call.class);

    when(secretManagerClient.deleteSecret(any(), any(), any(), any())).thenReturn(restResponseCall);
    when(restResponseCall.execute()).thenThrow(new IOException());

    try {
      ngSecretService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
      fail("Execution should not reach here");
    } catch (Exception ex) {
      // not required
    }
    verify(secretEntityReferenceHelper, times(0)).deleteSecretEntityReferenceWhenSecretGetsDeleted(any());
  }
}
