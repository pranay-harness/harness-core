/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.azure.registry;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;

import java.util.Map;
import java.util.Optional;

public class AzurePrivateRegistry extends AzureRegistry {
  @Override
  public Optional<DecryptableEntity> getAuthCredentialsDTO(ConnectorConfigDTO connectorConfigDTO) {
    ArtifactoryConnectorDTO artifactoryConnectorDTO = (ArtifactoryConnectorDTO) connectorConfigDTO;
    return artifactoryConnectorDTO.getAuth() != null
        ? Optional.ofNullable((ArtifactoryUsernamePasswordAuthDTO) artifactoryConnectorDTO.getAuth().getCredentials())
        : Optional.empty();
  }

  @Override
  public Map<String, AzureAppServiceApplicationSetting> getContainerSettings(ConnectorConfigDTO connectorConfigDTO) {
    ArtifactoryConnectorDTO artifactoryConnectorDTO = (ArtifactoryConnectorDTO) connectorConfigDTO;
    ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
        (ArtifactoryUsernamePasswordAuthDTO) artifactoryConnectorDTO.getAuth().getCredentials();

    String dockerRegistryUrl = artifactoryConnectorDTO.getArtifactoryServerUrl();
    String decryptedSecret = new String(artifactoryUsernamePasswordAuthDTO.getPasswordRef().getDecryptedValue());
    String username = artifactoryUsernamePasswordAuthDTO.getUsername();
    return populateDockerSettingMap(dockerRegistryUrl, username, decryptedSecret);
  }
}
