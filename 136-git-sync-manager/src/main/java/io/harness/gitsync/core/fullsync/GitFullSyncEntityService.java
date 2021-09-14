/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

@OwnedBy(DX)
public interface GitFullSyncEntityService {
  GitFullSyncEntityInfo save(GitFullSyncEntityInfo gitFullSyncEntityInfo);

  void markQueuedOrFailed(String messageId, String accountId, long currentRetryCount, long maxRetryCount);
}
