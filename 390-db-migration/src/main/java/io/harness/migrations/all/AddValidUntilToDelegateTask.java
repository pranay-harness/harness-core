/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddValidUntilToDelegateTask implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DelegateTask.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<DelegateTask> delegateTasks = new HIterator<>(wingsPersistence.createQuery(DelegateTask.class)
                                                                     .field("validUntil")
                                                                     .doesNotExist()
                                                                     .project(DelegateTaskKeys.createdAt, true)
                                                                     .fetch())) {
      while (delegateTasks.hasNext()) {
        final DelegateTask delegateTask = delegateTasks.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(delegateTask.getCreatedAt()).atZone(ZoneOffset.UTC).plusDays(7);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("DelegateTasks: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(DelegateTask.class)
                      .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject("validUntil", java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
