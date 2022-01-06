/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import static software.wings.yaml.gitSync.YamlChangeSet.Status.COMPLETED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.FAILED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.SKIPPED;

import io.harness.migrations.Migration;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteStaleYamlChangeSetsMigration implements Migration {
  public static final String BATCH_SIZE = "500";
  public static final int RETENTION_PERIOD_IN_DAYS = 30;

  @Inject WingsPersistence wingsPersistence;
  @Inject YamlChangeSetService yamlChangeSetService;
  @Inject AccountService accountService;
  @Override
  public void migrate() {
    log.info("Deleting stale YamlChangeSets");
    try {
      List<Account> accounts =
          accountService.list(wingsPersistence.query(Account.class, aPageRequest().addFieldsIncluded("_id").build()));
      for (Account account : accounts) {
        yamlChangeSetService.deleteChangeSets(account.getUuid(), new Status[] {COMPLETED, FAILED, SKIPPED},
            Integer.MAX_VALUE, BATCH_SIZE, RETENTION_PERIOD_IN_DAYS);
      }
    } catch (Exception e) {
      log.error("Delete YamlChangeSet error", e);
    }
  }
}
