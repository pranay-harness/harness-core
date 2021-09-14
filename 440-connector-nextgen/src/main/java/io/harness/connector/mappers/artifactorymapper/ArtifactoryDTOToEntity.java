/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.artifactorymapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector.ArtifactoryConnectorBuilder;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class ArtifactoryDTOToEntity
    implements ConnectorDTOToEntityMapper<ArtifactoryConnectorDTO, ArtifactoryConnector> {
  @Override
  public ArtifactoryConnector toConnectorEntity(ArtifactoryConnectorDTO configDTO) {
    ArtifactoryAuthType artifactoryAuthType = configDTO.getAuth().getAuthType();
    ArtifactoryConnectorBuilder artifactoryConnectorBuilder =
        ArtifactoryConnector.builder().url(configDTO.getArtifactoryServerUrl()).authType(artifactoryAuthType);
    if (artifactoryAuthType == ArtifactoryAuthType.USER_PASSWORD) {
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
          (ArtifactoryUsernamePasswordAuthDTO) configDTO.getAuth().getCredentials();
      artifactoryConnectorBuilder.artifactoryAuthentication(
          createArtifactoryAuthentication(artifactoryUsernamePasswordAuthDTO));
    }
    return artifactoryConnectorBuilder.build();
  }

  private ArtifactoryUserNamePasswordAuthentication createArtifactoryAuthentication(
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO) {
    return ArtifactoryUserNamePasswordAuthentication.builder()
        .username(artifactoryUsernamePasswordAuthDTO.getUsername())
        .usernameRef(SecretRefHelper.getSecretConfigString(artifactoryUsernamePasswordAuthDTO.getUsernameRef()))
        .passwordRef(SecretRefHelper.getSecretConfigString(artifactoryUsernamePasswordAuthDTO.getPasswordRef()))
        .build();
  }
}
