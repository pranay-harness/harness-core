/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cf.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.helper.DecryptionHelper;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.task.pcf.artifact.TasContainerArtifactConfig;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDP)
public class TasDockerHubPublicRegistrySettingsProvider extends AbstractTasRegistrySettingsProvider {
  @Override
  public TasArtifactCreds getContainerSettings(
      TasContainerArtifactConfig artifactConfig, DecryptionHelper decryptionHelper) {
    DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) artifactConfig.getConnectorConfig();
    String dockerRegistryUrl = dockerConnectorDTO.getDockerRegistryUrl();
    validateSettings(artifactConfig, dockerRegistryUrl);
    return populateDockerSettings(dockerRegistryUrl, null, null);
  }
}
