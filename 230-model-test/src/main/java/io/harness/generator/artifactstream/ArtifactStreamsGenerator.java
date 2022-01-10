/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;

import software.wings.beans.artifact.ArtifactStream;

@OwnedBy(CDP)
public interface ArtifactStreamsGenerator {
  ArtifactStream ensureArtifactStream(Seed seed, Owners owners);
  ArtifactStream ensureArtifactStream(Seed seed, Owners owners, String serviceName, boolean atConnector);
  ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector);
  ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly);
  ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream, Owners owners);
}
