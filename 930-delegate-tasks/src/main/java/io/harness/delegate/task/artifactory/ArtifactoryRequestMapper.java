/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifactory;

import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryConfigRequest.ArtifactoryConfigRequestBuilder;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;

import com.google.inject.Singleton;

@Singleton
public class ArtifactoryRequestMapper {
  public ArtifactoryConfigRequest toArtifactoryRequest(ArtifactoryConnectorDTO artifactoryConnector) {
    final ArtifactoryConfigRequestBuilder artifactoryConfigRequestBuilder =
        ArtifactoryConfigRequest.builder()
            .artifactoryUrl(artifactoryConnector.getArtifactoryServerUrl())
            .hasCredentials(false);
    if (artifactoryConnector.getAuth().getAuthType() == ArtifactoryAuthType.USER_PASSWORD) {
      final ArtifactoryUsernamePasswordAuthDTO credentials =
          (ArtifactoryUsernamePasswordAuthDTO) artifactoryConnector.getAuth().getCredentials();
      artifactoryConfigRequestBuilder.hasCredentials(true)
          .password(credentials.getPasswordRef().getDecryptedValue())
          .username(credentials.getUsername());
    }
    return artifactoryConfigRequestBuilder.build();
  }
}
