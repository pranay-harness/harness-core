/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.artifactstream;

import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.AzureMachineImageArtifactStream;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public abstract class AzureMachineImageArtifactStreamGenerator implements ArtifactStreamsGenerator {
  @Inject protected SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector) {
    return ensureArtifactStream(seed, owners, false, false);
  }

  protected ArtifactStream saveArtifactStream(ArtifactStream artifactStream, Owners owners) {
    AzureMachineImageArtifactStream azureMachineImageArtifactStream = (AzureMachineImageArtifactStream) artifactStream;
    ArtifactStream existing = artifactStreamGeneratorHelper.exists(azureMachineImageArtifactStream);
    if (existing != null) {
      return existing;
    }

    Preconditions.checkNotNull(artifactStream.getAppId());
    Preconditions.checkNotNull(artifactStream.getServiceId());
    Preconditions.checkNotNull(artifactStream.getName());

    return artifactStreamGeneratorHelper.saveArtifactStream(azureMachineImageArtifactStream, owners);
  }
}
