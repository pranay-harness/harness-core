/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddValidUntilToAlert implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(Alert.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<Alert> alerts = new HIterator<>(wingsPersistence.createQuery(Alert.class)
                                                       .field(AlertKeys.validUntil)
                                                       .doesNotExist()
                                                       .project(AlertKeys.closedAt, true)
                                                       .fetch())) {
      while (alerts.hasNext()) {
        final Alert alert = alerts.next();
        final ZonedDateTime zonedDateTime = alert.getClosedAt() == 0
            ? OffsetDateTime.now().plusMonths(1).toZonedDateTime()
            : Instant.ofEpochMilli(alert.getClosedAt()).atZone(ZoneOffset.UTC).plusDays(7);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("Alerts: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Alert.class).filter(AlertKeys.uuid, alert.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject(AlertKeys.validUntil, java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
