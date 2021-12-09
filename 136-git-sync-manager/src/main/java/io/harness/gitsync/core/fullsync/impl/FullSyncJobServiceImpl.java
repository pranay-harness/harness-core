package io.harness.gitsync.core.fullsync.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo.GitFullSyncEntityInfoKeys;
import io.harness.gitsync.core.fullsync.GitFullSyncEntityService;
import io.harness.gitsync.core.fullsync.GitFullSyncProcessorService;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob.GitFullSyncJobKeys;
import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob.SyncStatus;
import io.harness.gitsync.core.fullsync.service.FullSyncJobService;
import io.harness.repositories.fullSync.FullSyncJobRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class FullSyncJobServiceImpl implements FullSyncJobService {
  FullSyncJobRepository fullSyncJobRepository;
  GitFullSyncEntityService gitFullSyncEntityService;
  GitFullSyncProcessorService gitFullSyncProcessorService;

  @Override
  public GitFullSyncJob save(GitFullSyncJob gitFullSyncJob) {
    return fullSyncJobRepository.save(gitFullSyncJob);
  }

  @Override
  public void markFullSyncJobAsFailed(String accountIdentifier, String uuid, GitFullSyncJob.SyncStatus status) {
    Criteria criteria = new Criteria();
    criteria.and(GitFullSyncJobKeys.uuid).is(uuid);
    criteria.and(GitFullSyncJobKeys.accountIdentifier).is(accountIdentifier);
    Update update = new Update();
    update.set(GitFullSyncJobKeys.syncStatus, status);
    update.inc(GitFullSyncJobKeys.retryCount);
    fullSyncJobRepository.update(criteria, update);
  }

  @Override
  public void markFullSyncJobAsSuccess(String accountIdentifier, String uuid) {
    Criteria criteria = new Criteria();
    criteria.and(GitFullSyncEntityInfoKeys.uuid).is(uuid);
    criteria.and(GitFullSyncEntityInfoKeys.accountIdentifier).is(accountIdentifier);
    Update update = new Update();
    update.set(GitFullSyncEntityInfoKeys.syncStatus, SyncStatus.COMPLETED);
    fullSyncJobRepository.update(criteria, update);
  }

  @Override
  public GitFullSyncJob get(String accountIdentifier, String uuid) {
    GitFullSyncJob fullSyncJob = fullSyncJobRepository.findByAccountIdentifierAndUuid(accountIdentifier, uuid);
    if (fullSyncJob == null) {
      throw new InvalidRequestException("No full sync job exists with the id " + uuid);
    }
    return fullSyncJob;
  }
}
