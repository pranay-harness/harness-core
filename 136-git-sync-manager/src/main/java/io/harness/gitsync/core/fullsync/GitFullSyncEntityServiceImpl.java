package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo.GitFullSyncEntityInfoKeys;
import io.harness.repositories.fullSync.FullSyncRepository;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class GitFullSyncEntityServiceImpl implements GitFullSyncEntityService {
  FullSyncRepository fullSyncRepository;

  @Override
  public GitFullSyncEntityInfo save(GitFullSyncEntityInfo gitFullSyncEntityInfo) {
    return fullSyncRepository.save(gitFullSyncEntityInfo);
  }

  @Override
  public void markQueuedOrFailed(
      String messageId, String accountId, long currentRetryCount, long maxRetryCount, String errorMsg) {
    if (currentRetryCount + 1 < maxRetryCount) {
      incrementRetryCountAndMarkQueued(messageId, accountId, errorMsg);
    } else {
      markFailed(messageId, accountId, errorMsg);
    }
  }

  private void markFailed(String messageId, String accountId, String errorMsg) {
    Criteria criteria = new Criteria();
    criteria.and(GitFullSyncEntityInfoKeys.messageId).is(messageId);
    criteria.and(GitFullSyncEntityInfoKeys.accountIdentifier).is(accountId);
    Update update = new Update();
    update.set(GitFullSyncEntityInfoKeys.syncStatus, GitFullSyncEntityInfo.SyncStatus.FAILED);
    update.push(GitFullSyncEntityInfoKeys.errorMessage, errorMsg);
    fullSyncRepository.update(criteria, update);
  }

  private void incrementRetryCountAndMarkQueued(String messageId, String accountId, String errorMsg) {
    Criteria criteria = new Criteria();
    criteria.and(GitFullSyncEntityInfoKeys.messageId).is(messageId);
    criteria.and(GitFullSyncEntityInfoKeys.accountIdentifier).is(accountId);
    Update update = new Update();
    update.set(GitFullSyncEntityInfoKeys.syncStatus, GitFullSyncEntityInfo.SyncStatus.QUEUED);
    update.inc(GitFullSyncEntityInfoKeys.retryCount);
    update.push(GitFullSyncEntityInfoKeys.errorMessage, errorMsg);
    fullSyncRepository.update(criteria, update);
  }
}
