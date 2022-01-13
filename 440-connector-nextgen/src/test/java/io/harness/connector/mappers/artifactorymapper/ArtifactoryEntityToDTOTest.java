/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.artifactorymapper;

import static io.harness.encryption.Scope.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactoryEntityToDTOTest extends CategoryTest {
  @InjectMocks ArtifactoryEntityToDTO artifactoryEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void createConnectorDTOTest() {
    String artifactoryUrl = "url";
    String userName = "userName";
    String passwordRef = ACCOUNT.getYamlRepresentation() + ".passwordRef";

    ArtifactoryConnector artifactoryConnector =
        ArtifactoryConnector.builder()
            .authType(ArtifactoryAuthType.USER_PASSWORD)
            .url(artifactoryUrl)
            .artifactoryAuthentication(
                ArtifactoryUserNamePasswordAuthentication.builder().username(userName).passwordRef(passwordRef).build())
            .build();
    ArtifactoryConnectorDTO artifactoryConnectorDTO = artifactoryEntityToDTO.createConnectorDTO(artifactoryConnector);
    assertThat(artifactoryConnectorDTO).isNotNull();
    assertThat(artifactoryConnectorDTO.getArtifactoryServerUrl()).isEqualTo(artifactoryUrl);
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = artifactoryConnectorDTO.getAuth();
    assertThat(artifactoryAuthenticationDTO).isNotNull();
    assertThat(artifactoryAuthenticationDTO.getAuthType()).isEqualTo(ArtifactoryAuthType.USER_PASSWORD);
    ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
        (ArtifactoryUsernamePasswordAuthDTO) artifactoryAuthenticationDTO.getCredentials();
    assertThat(artifactoryUsernamePasswordAuthDTO).isNotNull();
    assertThat(artifactoryUsernamePasswordAuthDTO.getUsername()).isEqualTo(userName);
    assertThat(artifactoryUsernamePasswordAuthDTO.getPasswordRef())
        .isEqualTo(SecretRefHelper.createSecretRef(passwordRef));
  }
}
