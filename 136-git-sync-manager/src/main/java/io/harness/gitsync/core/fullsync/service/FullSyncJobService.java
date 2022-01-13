/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;

import com.mongodb.client.result.UpdateResult;
import java.util.Optional;

@OwnedBy(DX)
public interface FullSyncJobService {
  GitFullSyncJob save(GitFullSyncJob gitFullSyncJob);

  void markFullSyncJobAsFailed(String accountIdentifier, String uuid, GitFullSyncJob.SyncStatus status);

  void markFullSyncJobAsSuccess(String accountIdentifier, String uuid);

  UpdateResult markJobAsRunning(String accountIdentifier, String uuid);

  Optional<GitFullSyncJob> getRunningOrQueuedJob(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
