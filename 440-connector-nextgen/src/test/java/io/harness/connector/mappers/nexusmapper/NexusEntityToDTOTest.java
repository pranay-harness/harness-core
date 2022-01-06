/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.nexusmapper;

import static io.harness.encryption.Scope.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDC)
public class NexusEntityToDTOTest extends CategoryTest {
  @InjectMocks NexusEntityToDTO nexusEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void createConnectorDTOTest() {
    String nexusUrl = "url";
    String userName = "userName";
    String passwordRef = ACCOUNT.getYamlRepresentation() + ".passwordRef";
    final String version = "1.2";

    NexusConnector nexusConnector =
        NexusConnector.builder()
            .authType(NexusAuthType.USER_PASSWORD)
            .url(nexusUrl)
            .nexusVersion(version)
            .nexusAuthentication(
                NexusUserNamePasswordAuthentication.builder().username(userName).passwordRef(passwordRef).build())
            .build();
    NexusConnectorDTO nexusConnectorDTO = nexusEntityToDTO.createConnectorDTO(nexusConnector);
    assertThat(nexusConnectorDTO).isNotNull();
    assertThat(nexusConnectorDTO.getNexusServerUrl()).isEqualTo(nexusUrl);
    assertThat(nexusConnectorDTO.getVersion()).isEqualTo(version);
    NexusAuthenticationDTO nexusAuthenticationDTO = nexusConnectorDTO.getAuth();
    assertThat(nexusAuthenticationDTO).isNotNull();
    assertThat(nexusAuthenticationDTO.getAuthType()).isEqualTo(NexusAuthType.USER_PASSWORD);
    NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
        (NexusUsernamePasswordAuthDTO) nexusAuthenticationDTO.getCredentials();
    assertThat(nexusUsernamePasswordAuthDTO).isNotNull();
    assertThat(nexusUsernamePasswordAuthDTO.getUsername()).isEqualTo(userName);
    assertThat(nexusUsernamePasswordAuthDTO.getPasswordRef()).isEqualTo(SecretRefHelper.createSecretRef(passwordRef));
  }
}
