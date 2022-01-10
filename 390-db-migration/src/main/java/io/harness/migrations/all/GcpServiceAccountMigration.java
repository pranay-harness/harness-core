/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.config.GcpServiceAccount;
import io.harness.migrations.Migration;

import software.wings.beans.ce.depricated.GcpServiceAccountOld;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GcpServiceAccountMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      log.info("Starting migration of all CE gcpServiceAccounts");

      List<GcpServiceAccountOld> gcpServiceAccountOldList =
          wingsPersistence.createQuery(GcpServiceAccountOld.class, excludeValidate).asList();
      List<GcpServiceAccount> gcpServiceAccountForMigration = new ArrayList<>();
      gcpServiceAccountOldList.forEach(gcpServiceAccountOld
          -> gcpServiceAccountForMigration.add(GcpServiceAccount.builder()
                                                   .uuid(gcpServiceAccountOld.getUuid())
                                                   .accountId(gcpServiceAccountOld.getAccountId())
                                                   .serviceAccountId(gcpServiceAccountOld.getServiceAccountId())
                                                   .gcpUniqueId(gcpServiceAccountOld.getGcpUniqueId())
                                                   .email(gcpServiceAccountOld.getEmail())
                                                   .createdAt(gcpServiceAccountOld.getCreatedAt())
                                                   .lastUpdatedAt(gcpServiceAccountOld.getLastUpdatedAt())
                                                   .build()));

      if (!gcpServiceAccountForMigration.isEmpty()) {
        wingsPersistence.save(gcpServiceAccountForMigration);
        log.info("Migrated gcpServiceAccount size {}", gcpServiceAccountForMigration.size());
      }
    } catch (Exception e) {
      log.error("Failure occurred in gcpServiceAccountForMigration", e);
    }
    log.info("gcpServiceAccountForMigration has completed");
  }
}
