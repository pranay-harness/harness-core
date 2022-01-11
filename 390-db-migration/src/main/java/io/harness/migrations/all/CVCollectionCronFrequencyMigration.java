/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CVCollectionCronFrequencyMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "quartz_verification_triggers");
    final WriteResult result = collection.updateMulti(new BasicDBObject("keyGroup", "METRIC_DATA_PROCESSOR_CRON_GROUP"),
        new BasicDBObject("$set", new BasicDBObject("repeatInterval", Long.valueOf(30000))));

    log.info("updated {} records", result.getN());
    log.info("migration done...");
  }
}
