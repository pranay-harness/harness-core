package io.harness.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DX)
public class ArtifactDetails {
  private String artifactId;
  private String tag; // this corresponds to the build number of the artifact
}
