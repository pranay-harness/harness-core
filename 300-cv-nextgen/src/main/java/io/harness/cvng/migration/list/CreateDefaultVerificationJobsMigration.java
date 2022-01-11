/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class CreateDefaultVerificationJobsMigration implements CVNGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobService verificationJobService;

  @Override
  public void migrate() {
    Set<Pair<String, Pair<String, String>>> accountOrgProjectId = new TreeSet<>();
    hPersistence.delete(hPersistence.createQuery(VerificationJob.class).filter(VerificationJobKeys.isDefaultJob, true));
    List<CVConfig> cvConfigs = hPersistence.createQuery(CVConfig.class).asList();
    cvConfigs.forEach(config
        -> accountOrgProjectId.add(new ImmutablePair<>(
            config.getAccountId(), new ImmutablePair<>(config.getOrgIdentifier(), config.getProjectIdentifier()))));
    List<VerificationJob> verificationJobs = hPersistence.createQuery(VerificationJob.class).asList();
    verificationJobs.forEach(verificationJob
        -> accountOrgProjectId.add(new ImmutablePair<>(verificationJob.getAccountId(),
            new ImmutablePair<>(verificationJob.getOrgIdentifier(), verificationJob.getProjectIdentifier()))));
    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class).asList();
    activitySources.forEach(activitySource
        -> accountOrgProjectId.add(new ImmutablePair<>(activitySource.getAccountId(),
            new ImmutablePair<>(activitySource.getOrgIdentifier(), activitySource.getProjectIdentifier()))));

    accountOrgProjectId.forEach(accountOrgProject
        -> verificationJobService.createDefaultVerificationJobs(accountOrgProject.getLeft(),
            accountOrgProject.getRight().getLeft(), accountOrgProject.getRight().getRight()));
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.builder()
        .desc(
            "This is a migration to introduce default verification jobs so after rollback old version will have default jobs.")
        .build();
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.builder().desc("Verification job does not have any iterator.").build();
  }
}
