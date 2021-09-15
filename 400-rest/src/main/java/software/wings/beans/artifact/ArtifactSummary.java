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
public class ArtifactSummary {
  private String uuid;
  private String uiDisplayName;
  private String buildNo;

  public static ArtifactSummary prepareSummaryFromArtifact(Artifact artifact) {
    if (artifact == null) {
      return null;
    }

    return ArtifactSummary.builder()
        .uuid(artifact.getUuid())
        .uiDisplayName(artifact.getUiDisplayName())
        .buildNo(artifact.getBuildNo())
        .build();
  }
}
