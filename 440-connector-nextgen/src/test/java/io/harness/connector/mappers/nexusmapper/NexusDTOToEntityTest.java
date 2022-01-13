/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.nexusmapper;

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
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDC)
public class NexusDTOToEntityTest extends CategoryTest {
  @InjectMocks NexusDTOToEntity nexusDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void toConnectorEntityTest() {
    String url = "url";
    String userName = "userName";
    String passwordRefIdentifier = "passwordRefIdentifier";
    final String version = "1.2";

    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
        NexusUsernamePasswordAuthDTO.builder().username(userName).passwordRef(passwordSecretRef).build();

    NexusAuthenticationDTO nexusAuthenticationDTO = NexusAuthenticationDTO.builder()
                                                        .authType(NexusAuthType.USER_PASSWORD)
                                                        .credentials(nexusUsernamePasswordAuthDTO)
                                                        .build();
    NexusConnectorDTO nexusConnectorDTO =
        NexusConnectorDTO.builder().nexusServerUrl(url).version(version).auth(nexusAuthenticationDTO).build();
    NexusConnector nexusConnector = nexusDTOToEntity.toConnectorEntity(nexusConnectorDTO);
    assertThat(nexusConnector).isNotNull();
    assertThat(nexusConnector.getUrl()).isEqualTo(url);
    assertThat(nexusConnector.getNexusVersion()).isEqualTo(version);
    assertThat(((NexusUserNamePasswordAuthentication) (nexusConnector.getNexusAuthentication())).getUsername())
        .isEqualTo(userName);
    assertThat(((NexusUserNamePasswordAuthentication) (nexusConnector.getNexusAuthentication())).getPasswordRef())
        .isEqualTo(passwordSecretRef.toSecretRefStringValue());
    assertThat(nexusConnector.getAuthType()).isEqualTo(NexusAuthType.USER_PASSWORD);
  }
}
