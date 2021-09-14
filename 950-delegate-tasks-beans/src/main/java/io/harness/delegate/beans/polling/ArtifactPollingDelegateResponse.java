/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class ArtifactPollingDelegateResponse implements PollingResponseInfc {
  private List<ArtifactDelegateResponse> unpublishedArtifacts;
  private Set<String> toBeDeletedKeys;
  boolean firstCollectionOnDelegate;
}
