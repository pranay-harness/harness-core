/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
