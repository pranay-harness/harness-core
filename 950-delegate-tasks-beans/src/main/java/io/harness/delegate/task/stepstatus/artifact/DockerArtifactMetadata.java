/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.stepstatus.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CI)
public class DockerArtifactMetadata implements ArtifactMetadataSpec {
  String registryType;
  String registryUrl;
  @Singular List<DockerArtifactDescriptor> dockerArtifacts;
}
