/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.artifactorymapper;

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
public class ArtifactoryDTOToEntityTest extends CategoryTest {
  @InjectMocks ArtifactoryDTOToEntity artifactoryDTOToEntity;

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
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
        ArtifactoryUsernamePasswordAuthDTO.builder().username(userName).passwordRef(passwordSecretRef).build();

    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder()
                                                                    .authType(ArtifactoryAuthType.USER_PASSWORD)
                                                                    .credentials(artifactoryUsernamePasswordAuthDTO)
                                                                    .build();
    ArtifactoryConnectorDTO artifactoryConnectorDTO =
        ArtifactoryConnectorDTO.builder().artifactoryServerUrl(url).auth(artifactoryAuthenticationDTO).build();
    ArtifactoryConnector artifactoryConnector = artifactoryDTOToEntity.toConnectorEntity(artifactoryConnectorDTO);
    assertThat(artifactoryConnector).isNotNull();
    assertThat(artifactoryConnector.getUrl()).isEqualTo(url);
    assertThat(((ArtifactoryUserNamePasswordAuthentication) (artifactoryConnector.getArtifactoryAuthentication()))
                   .getUsername())
        .isEqualTo(userName);
    assertThat(((ArtifactoryUserNamePasswordAuthentication) (artifactoryConnector.getArtifactoryAuthentication()))
                   .getPasswordRef())
        .isEqualTo(passwordSecretRef.toSecretRefStringValue());
    assertThat(artifactoryConnector.getAuthType()).isEqualTo(ArtifactoryAuthType.USER_PASSWORD);
  }
}
