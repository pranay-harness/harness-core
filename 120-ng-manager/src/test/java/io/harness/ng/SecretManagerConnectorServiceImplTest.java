package io.harness.ng;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConnectorDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Response;
import software.wings.service.impl.security.SecretManagementException;

import java.io.IOException;

public class SecretManagerConnectorServiceImplTest extends CategoryTest {
  private static final String ACCOUNT = "account";
  private SecretManagerClient secretManagerClient;
  private ConnectorService defaultConnectorService;
  private SecretManagerConnectorServiceImpl secretManagerConnectorService;

  @Before
  public void setup() {
    secretManagerClient = mock(SecretManagerClient.class, RETURNS_DEEP_STUBS);
    defaultConnectorService = mock(ConnectorService.class);
    secretManagerConnectorService = new SecretManagerConnectorServiceImpl(defaultConnectorService, secretManagerClient);
  }

  private ConnectorRequestDTO getRequestDTO() {
    ConnectorRequestDTO connectorRequestDTO = ConnectorRequestDTO.builder().build();
    connectorRequestDTO.setConnectorType(ConnectorType.VAULT);
    connectorRequestDTO.setConnectorConfig(VaultConnectorDTO.builder().build());
    connectorRequestDTO.setName("name");
    connectorRequestDTO.setIdentifier("identifier");
    return connectorRequestDTO;
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManagerConnector() throws IOException {
    SecretManagerConfigDTO secretManagerConfigDTO = random(VaultConfigDTO.class);
    when(secretManagerClient.createSecretManager(any()).execute())
        .thenReturn(Response.success(new RestResponse<>(secretManagerConfigDTO)));
    when(defaultConnectorService.create(any(), any())).thenReturn(null);
    ConnectorDTO connectorDTO = secretManagerConnectorService.create(getRequestDTO(), ACCOUNT);
    assertThat(connectorDTO).isEqualTo(null);
    verify(defaultConnectorService).create(any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManagerConnectorShouldFail_ManagerReturnsNull() throws IOException {
    when(secretManagerClient.createSecretManager(any()).execute())
        .thenReturn(Response.success(new RestResponse<>(null)));
    try {
      secretManagerConnectorService.create(getRequestDTO(), ACCOUNT);
      fail("Should fail if execution reaches here");
    } catch (SecretManagementException exception) {
      // do nothing
    }
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManagerConnectorShouldFail_exceptionFromManager() throws IOException {
    SecretManagerConfigDTO secretManagerConfigDTO = random(VaultConfigDTO.class);
    when(secretManagerClient.createSecretManager(any()).execute())
        .thenReturn(Response.error(400, ResponseBody.create(MediaType.parse("application/json"), "error".getBytes())));

    try {
      secretManagerConnectorService.create(getRequestDTO(), ACCOUNT);
      fail("Should fail if execution reaches here");
    } catch (InvalidRequestException exception) {
      // do nothing
    }
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManagerShouldFail_exceptionWhileSavingConnector() throws IOException {
    SecretManagerConfigDTO secretManagerConfigDTO = random(VaultConfigDTO.class);
    when(secretManagerClient.createSecretManager(any()).execute())
        .thenReturn(Response.success(new RestResponse<>(secretManagerConfigDTO)));
    when(defaultConnectorService.create(any(), any())).thenThrow(new InvalidRequestException("error"));
    when(secretManagerClient.deleteSecret(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));
    try {
      secretManagerConnectorService.create(getRequestDTO(), ACCOUNT);
      fail("Should fail if execution reaches here");
    } catch (SecretManagementException exception) {
      // do nothing
    }

    /* if you are figuring out why this verification contains atLeastOnce(),
     with RETURNS_DEEP_STUBS, verify call will read how long your chain is
     For example: if you have mocked this call -> when(x.get().something()).thenReturn(blah_blah)
     when you call verify(x).get(), it will fail and say that x.get() was called 2 times
     */
    verify(secretManagerClient, atLeastOnce()).deleteSecret(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void updateSecretManager() throws IOException {
    when(secretManagerClient.updateSecretManager(any(), any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(random(VaultConfigDTO.class))));
    when(defaultConnectorService.update(any(), any())).thenReturn(null);
    ConnectorDTO connectorDTO = secretManagerConnectorService.update(getRequestDTO(), ACCOUNT);
    assertThat(connectorDTO).isEqualTo(null);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testDeleteSecretManager() throws IOException {
    when(secretManagerClient.deleteSecretManager(any(), any(), any(), any()).execute())
        .thenReturn(Response.success(new RestResponse<>(true)));
    when(defaultConnectorService.delete(any(), any(), any(), any())).thenReturn(true);
    boolean success = secretManagerConnectorService.delete(ACCOUNT, null, null, "identifier");
    assertThat(success).isEqualTo(true);
  }
}
