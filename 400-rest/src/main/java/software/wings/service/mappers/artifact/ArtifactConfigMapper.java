/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.mappers.artifact;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;

import software.wings.helpers.ext.jenkins.BuildDetails;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class ArtifactConfigMapper {
  public BuildDetails toBuildDetails(BuildDetailsInternal buildDetailsInternal) {
    return aBuildDetails()
        .withNumber(buildDetailsInternal.getNumber())
        .withBuildUrl(buildDetailsInternal.getBuildUrl())
        .withMetadata(buildDetailsInternal.getMetadata())
        .withUiDisplayName(buildDetailsInternal.getUiDisplayName())
        .build();
  }
}
