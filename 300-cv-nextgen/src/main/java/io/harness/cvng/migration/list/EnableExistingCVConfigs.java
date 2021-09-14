/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.migration.list;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;

public class EnableExistingCVConfigs implements CVNGMigration {
  @Inject private HPersistence hPersistence;

  @Override
  public void migrate() {
    hPersistence.update(hPersistence.createQuery(CVConfig.class).field(CVConfigKeys.enabled).doesNotExist(),
        hPersistence.createUpdateOperations(CVConfig.class).set(CVConfigKeys.enabled, true));
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.builder()
        .desc("This is a new field that is getting added to CVConfig so rollback won't have any impact.")
        .build();
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.builder()
        .desc("This is a new field that is getting added to CVConfig so rollback won't have any impact.")
        .build();
  }
}
