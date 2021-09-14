/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@OwnedBy(HarnessTeam.DX)
@FieldNameConstants(innerTypeName = "ArtifactDetailsKeys")
public class ArtifactDetails {
  private String artifactId;
  private String tag; // this corresponds to the build number of the artifact
}
