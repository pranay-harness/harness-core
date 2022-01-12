/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.appdynamicsconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsAuthType;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CV)
public class AppDynamicsEntityToDTOTest extends CategoryTest {
  @InjectMocks AppDynamicsEntityToDTO appDynamicsEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnectorDTO() {
    String username = "username";
    String passwordRef = "passwordRef";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConnector appDynamicsConnector = AppDynamicsConnector.builder()
                                                    .username(username)
                                                    .passwordRef(passwordRef)
                                                    .accountname(accountname)
                                                    .authType(AppDynamicsAuthType.USERNAME_PASSWORD)
                                                    .controllerUrl(controllerUrl)
                                                    .build();

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = appDynamicsEntityToDTO.createConnectorDTO(appDynamicsConnector);
    assertThat(appDynamicsConnectorDTO).isNotNull();
    assertThat(appDynamicsConnectorDTO.getUsername()).isEqualTo(appDynamicsConnector.getUsername());
    assertThat(appDynamicsConnectorDTO.getPasswordRef()).isNotNull();
    assertThat(appDynamicsConnectorDTO.getPasswordRef().getIdentifier())
        .isEqualTo(appDynamicsConnector.getPasswordRef());
    assertThat(appDynamicsConnectorDTO.getAccountname()).isEqualTo(appDynamicsConnector.getAccountname());
    assertThat(appDynamicsConnectorDTO.getControllerUrl()).isEqualTo(appDynamicsConnector.getControllerUrl() + "/");

    assertThat(appDynamicsConnectorDTO.getAuthType().name()).isEqualTo(AppDynamicsAuthType.USERNAME_PASSWORD.name());
    assertThat(appDynamicsConnectorDTO.getClientId()).isNull();
    assertThat(appDynamicsConnectorDTO.getClientSecretRef().getIdentifier()).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnectorDTO_withClientId() {
    String username = "username";
    String passwordRef = "passwordRef";
    String clientId = "testClientId";
    String clientSecret = "clientSecretRef";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConnector appDynamicsConnector = AppDynamicsConnector.builder()
                                                    .clientId(clientId)
                                                    .clientSecret(clientSecret)
                                                    .authType(AppDynamicsAuthType.API_CLIENT_TOKEN)
                                                    .accountname(accountname)
                                                    .controllerUrl(controllerUrl)
                                                    .build();

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = appDynamicsEntityToDTO.createConnectorDTO(appDynamicsConnector);
    assertThat(appDynamicsConnectorDTO).isNotNull();
    assertThat(appDynamicsConnectorDTO.getUsername()).isNull();
    assertThat(appDynamicsConnectorDTO.getPasswordRef()).isNotNull();
    assertThat(appDynamicsConnectorDTO.getPasswordRef().getIdentifier()).isNull();
    assertThat(appDynamicsConnectorDTO.getAccountname()).isEqualTo(appDynamicsConnector.getAccountname());
    assertThat(appDynamicsConnectorDTO.getControllerUrl()).isEqualTo(appDynamicsConnector.getControllerUrl() + "/");
    assertThat(appDynamicsConnectorDTO.getAuthType().name()).isEqualTo(AppDynamicsAuthType.API_CLIENT_TOKEN.name());
    assertThat(appDynamicsConnectorDTO.getClientId()).isEqualTo(appDynamicsConnector.getClientId());
    assertThat(appDynamicsConnectorDTO.getClientSecretRef().getIdentifier())
        .isEqualTo(appDynamicsConnector.getClientSecret());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnectorDTO_invalidWithClientId() {
    String username = "username";
    String passwordRef = "passwordRef";
    String clientId = "testClientId";
    String clientSecret = "clientSecretRef";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConnector appDynamicsConnector = AppDynamicsConnector.builder()
                                                    .clientId(null)
                                                    .clientSecret(clientSecret)
                                                    .authType(AppDynamicsAuthType.API_CLIENT_TOKEN)
                                                    .accountname(accountname)
                                                    .controllerUrl(controllerUrl)
                                                    .build();

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = appDynamicsEntityToDTO.createConnectorDTO(appDynamicsConnector);
    assertThatThrownBy(() -> appDynamicsConnectorDTO.validate())
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Client ID or Client Secret cannot be empty for ApiClientToken Auth type");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnectorDTO_invalidWithUsername() {
    String username = "username";
    String passwordRef = "passwordRef";
    String clientId = "testClientId";
    String clientSecret = "clientSecretRef";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConnector appDynamicsConnector = AppDynamicsConnector.builder()
                                                    .username(null)
                                                    .passwordRef(passwordRef)
                                                    .authType(AppDynamicsAuthType.USERNAME_PASSWORD)
                                                    .accountname(accountname)
                                                    .controllerUrl(controllerUrl)
                                                    .build();

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = appDynamicsEntityToDTO.createConnectorDTO(appDynamicsConnector);
    assertThatThrownBy(() -> appDynamicsConnectorDTO.validate())
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Username and Password cannot be empty for UsernamePassword Auth type");
  }
}
