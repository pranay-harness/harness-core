/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo.GitFullSyncEntityInfoKeys;
import io.harness.repositories.fullSync.FullSyncRepository;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
public class GitFullSyncEntityServiceImpl implements GitFullSyncEntityService {
  FullSyncRepository fullSyncRepository;

  @Override
  public GitFullSyncEntityInfo save(GitFullSyncEntityInfo gitFullSyncEntityInfo) {
    return fullSyncRepository.save(gitFullSyncEntityInfo);
  }

  @Override
  public void markQueuedOrFailed(String messageId, String accountId, long currentRetryCount, long maxRetryCount) {
    if (currentRetryCount + 1 < maxRetryCount) {
      incrementRetryCountAndMarkQueued(messageId, accountId);
    } else {
      markFailed(messageId, accountId);
    }
  }

  private void markFailed(String messageId, String accountId) {
    Criteria criteria = new Criteria();
    criteria.and(GitFullSyncEntityInfoKeys.messageId).is(messageId);
    criteria.and(GitFullSyncEntityInfoKeys.accountIdentifier).is(accountId);
    Update update = new Update();
    update.set(GitFullSyncEntityInfoKeys.syncStatus, GitFullSyncEntityInfo.SyncStatus.FAILED);
    fullSyncRepository.update(criteria, update);
  }

  private void incrementRetryCountAndMarkQueued(String messageId, String accountId) {
    Criteria criteria = new Criteria();
    criteria.and(GitFullSyncEntityInfoKeys.messageId).is(messageId);
    criteria.and(GitFullSyncEntityInfoKeys.accountIdentifier).is(accountId);
    Update update = new Update();
    update.set(GitFullSyncEntityInfoKeys.syncStatus, GitFullSyncEntityInfo.SyncStatus.QUEUED);
    update.inc(GitFullSyncEntityInfoKeys.retryCount);
    fullSyncRepository.update(criteria, update);
  }
}
