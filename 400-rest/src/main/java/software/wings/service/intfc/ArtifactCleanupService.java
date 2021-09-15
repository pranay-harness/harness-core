/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.ArtifactStream;

@OwnedBy(CDC)
public interface ArtifactCleanupService {
  void cleanupArtifacts(ArtifactStream artifactStream, String accountId);
}
