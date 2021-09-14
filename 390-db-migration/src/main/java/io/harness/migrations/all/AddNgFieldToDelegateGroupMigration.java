/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroup.DelegateGroupKeys;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class AddNgFieldToDelegateGroupMigration implements Migration {
  @Inject protected WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("adding ng field to delegate group");
    UpdateResults updateResults = wingsPersistence.update(
        wingsPersistence.createQuery(DelegateGroup.class, excludeAuthority).field(DelegateGroupKeys.ng).doesNotExist(),
        wingsPersistence.createUpdateOperations(DelegateGroup.class).set(DelegateGroupKeys.ng, false));
    log.info("updated {} records", updateResults.getUpdatedCount());
  }
}
