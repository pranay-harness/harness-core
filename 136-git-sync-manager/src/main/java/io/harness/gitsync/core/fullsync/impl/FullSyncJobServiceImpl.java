/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
  private static final List<SyncStatus> runningStatuses =
      Arrays.asList(SyncStatus.QUEUED, SyncStatus.FAILED_WITH_RETRIES_LEFT);

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
  public Optional<GitFullSyncJob> getRunningJobs(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = Criteria.where(GitFullSyncJobKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(GitFullSyncJobKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(GitFullSyncJobKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(GitFullSyncJobKeys.syncStatus)
                            .in(runningStatuses);
    return Optional.ofNullable(fullSyncJobRepository.find(criteria));
  }
}
