/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OwnedByArtifactStream {
  /**
   * Prune if belongs to artifact stream.
   *
   * @param appId the app id
   * @param artifactStreamId the artifact stream id
   */
  void pruneByArtifactStream(String appId, String artifactStreamId);
}
