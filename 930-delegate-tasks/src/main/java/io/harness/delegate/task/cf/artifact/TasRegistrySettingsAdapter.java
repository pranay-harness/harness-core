/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cf.artifact;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.helper.DecryptionHelper;
import io.harness.delegate.task.pcf.artifact.TasContainerArtifactConfig;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NestedExceptionUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@OwnedBy(HarnessTeam.CDP)
public class TasRegistrySettingsAdapter {
  @Inject private TasDockerHubPublicRegistrySettingsProvider dockerHubPublicRegistrySettingsProvider;
  @Inject private TasDockerHubPrivateRegistrySettingsProvider dockerHubPrivateRegistrySettingsProvider;
  @Inject private TasArtifactoryRegistrySettingsProvider artifactoryRegistrySettingsProvider;
  @Inject private TasContainerRegistrySettingsProvider tasContainerRegistrySettingsProvider;
  @Inject private TasElasticContainerRegistrySettingsProvider tasElasticContainerRegistrySettingsProvider;
  @Inject private TasGoogleContainerRegistrySettingsProvider tasGoogleContainerRegistrySettingsProvider;
  @Inject private TasNexus3RegistrySettingsProvider tasNexus3RegistrySettingsProvider;
  @Inject DecryptionHelper decryptionHelper;

  public TasArtifactCreds getContainerSettings(TasContainerArtifactConfig artifactConfig) {
    switch (artifactConfig.getRegistryType()) {
      case DOCKER_HUB_PUBLIC:
        return dockerHubPublicRegistrySettingsProvider.getContainerSettings(artifactConfig, decryptionHelper);
      case DOCKER_HUB_PRIVATE:
        return dockerHubPrivateRegistrySettingsProvider.getContainerSettings(artifactConfig, decryptionHelper);
      case ARTIFACTORY_PRIVATE_REGISTRY:
        return artifactoryRegistrySettingsProvider.getContainerSettings(artifactConfig, decryptionHelper);
      case ACR:
        return tasContainerRegistrySettingsProvider.getContainerSettings(artifactConfig, decryptionHelper);
      case ECR:
        return tasElasticContainerRegistrySettingsProvider.getContainerSettings(artifactConfig, decryptionHelper);
      case GCR:
        return tasGoogleContainerRegistrySettingsProvider.getContainerSettings(artifactConfig, decryptionHelper);
      case NEXUS_PRIVATE_REGISTRY:
        return tasNexus3RegistrySettingsProvider.getContainerSettings(artifactConfig, decryptionHelper);
      default:
        throw NestedExceptionUtils.hintWithExplanationException(
            "Use a different container registry supported by Harness",
            format("Container registry of type '%s' is not supported", artifactConfig.getRegistryType().getValue()),
            new InvalidArgumentsException(Pair.of("registryType", "Unsupported registry type")));
    }
  }
}
