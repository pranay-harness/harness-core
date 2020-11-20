package io.harness.ng;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class SecretManagerConnectorServiceImplTest extends CategoryTest {
  private static final String ACCOUNT = "account";
  private NGSecretManagerService ngSecretManagerService;
  private ConnectorService defaultConnectorService;
  private SecretManagerConnectorServiceImpl secretManagerConnectorService;

  @Before
  public void setup() {
    ngSecretManagerService = mock(NGSecretManagerService.class);
    defaultConnectorService = mock(ConnectorService.class);
    secretManagerConnectorService =
        new SecretManagerConnectorServiceImpl(defaultConnectorService, ngSecretManagerService);
  }

  private InvalidRequestException getInvalidRequestException() {
    return new InvalidRequestException("Invalid request");
  }

  private ConnectorDTO getRequestDTO() {
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder().build();
    connectorInfo.setConnectorType(ConnectorType.VAULT);
    connectorInfo.setConnectorConfig(VaultConnectorDTO.builder().build());
    connectorInfo.setName("name");
    connectorInfo.setIdentifier("identifier");
    return ConnectorDTO.builder().connectorInfo(connectorInfo).build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManagerConnector() throws IOException {
    SecretManagerConfigDTO secretManagerConfigDTO = random(VaultConfigDTO.class);
    when(ngSecretManagerService.createSecretManager(any())).thenReturn(secretManagerConfigDTO);
    when(defaultConnectorService.create(any(), any())).thenReturn(null);
    ConnectorResponseDTO connectorDTO = secretManagerConnectorService.create(getRequestDTO(), ACCOUNT);
    assertThat(connectorDTO).isEqualTo(null);
    verify(defaultConnectorService).create(any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManagerConnectorShouldFail_ManagerReturnsNull() throws IOException {
    when(ngSecretManagerService.createSecretManager(any())).thenReturn(null);
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
    when(ngSecretManagerService.createSecretManager(any())).thenThrow(getInvalidRequestException());

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
    when(ngSecretManagerService.createSecretManager(any())).thenReturn(secretManagerConfigDTO);
    when(defaultConnectorService.create(any(), any())).thenThrow(new InvalidRequestException("error"));
    when(ngSecretManagerService.deleteSecretManager(any(), any(), any(), any())).thenReturn(true);
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
    verify(ngSecretManagerService, atLeastOnce()).deleteSecretManager(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void updateSecretManager() throws IOException {
    when(ngSecretManagerService.updateSecretManager(any(), any(), any(), any(), any()))
        .thenReturn(random(VaultConfigDTO.class));
    when(defaultConnectorService.update(any(), any())).thenReturn(null);
    ConnectorResponseDTO connectorDTO = secretManagerConnectorService.update(getRequestDTO(), ACCOUNT);
    assertThat(connectorDTO).isEqualTo(null);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testDeleteSecretManager() throws IOException {
    when(ngSecretManagerService.deleteSecretManager(any(), any(), any(), any())).thenReturn(true);
    when(defaultConnectorService.delete(any(), any(), any(), any())).thenReturn(true);
    boolean success = secretManagerConnectorService.delete(ACCOUNT, null, null, "identifier");
    assertThat(success).isEqualTo(true);
  }
}
