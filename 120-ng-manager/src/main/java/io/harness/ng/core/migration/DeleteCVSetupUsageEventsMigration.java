/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.migration;

import static io.harness.EntityType.CV_CONFIG;
import static io.harness.EntityType.CV_KUBERNETES_ACTIVITY_SOURCE;
import static io.harness.EntityType.CV_VERIFICATION_JOB;
import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CV)
public class DeleteCVSetupUsageEventsMigration implements NGMigration {
  @Inject private EntitySetupUsageService entitySetupUsageService;

  @Override
  public void migrate() {
    log.info("Starting migration for cv setup usage events");
    try {
      entitySetupUsageService.deleteByReferredByEntityType(CV_CONFIG);
      entitySetupUsageService.deleteByReferredByEntityType(CV_VERIFICATION_JOB);
      entitySetupUsageService.deleteByReferredByEntityType(CV_KUBERNETES_ACTIVITY_SOURCE);
    } catch (Exception ex) {
      log.error("Exception while running cv setup usage events migration", ex);
    }
    log.info("Finish migration for cv setup usage events");
  }
}
