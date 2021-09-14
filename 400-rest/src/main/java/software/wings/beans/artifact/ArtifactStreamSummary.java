/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Value
@Builder
public class ArtifactStreamSummary {
  private String artifactStreamId;
  private String settingId;
  private String displayName;
  private String name;
  private String lastCollectedArtifact;
  private ArtifactSummary defaultArtifact;

  public static ArtifactStreamSummary prepareSummaryFromArtifactStream(
      ArtifactStream artifactStream, Artifact lastCollectedArtifact) {
    if (artifactStream == null) {
      return null;
    }

    String lastCollectedArtifactName = null;
    if (lastCollectedArtifact != null) {
      lastCollectedArtifactName = lastCollectedArtifact.getBuildNo();
    }
    return ArtifactStreamSummary.builder()
        .artifactStreamId(artifactStream.getUuid())
        .settingId(artifactStream.getSettingId())
        .displayName(artifactStream.getName())
        .lastCollectedArtifact(lastCollectedArtifactName)
        .name(artifactStream.getName())
        .build();
  }
}
