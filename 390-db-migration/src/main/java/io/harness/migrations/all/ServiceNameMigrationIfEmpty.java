/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceNameMigrationIfEmpty implements Migration {
  public static final String HARNESS_SERVICE = "HARNESS_SERVICE";

  @Inject private WingsPersistence wingsPersistence;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    log.info("Changing Empty Service Name to Harness_Service");
    final DBCollection collection = wingsPersistence.getCollection(Service.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 1;
    int count = 1;
    log.info("Migrating Services with No Name");
    try (HIterator<Service> services =
             new HIterator<>(wingsPersistence.createQuery(Service.class).project(ServiceKeys.name, true).fetch())) {
      for (Service service : services) {
        if (i % 50 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("Services: {} updated", i);
        }
        if (isEmpty(service.getName()) || isEmpty(service.getName().trim())) {
          bulkWriteOperation
              .find(wingsPersistence.createQuery(Service.class).filter(Service.ID, service.getUuid()).getQueryObject())
              .updateOne(new BasicDBObject("$set",
                  new BasicDBObject(ServiceKeys.name,
                      new StringBuilder(128).append(HARNESS_SERVICE).append("_0").append(count).toString())));
          ++count;
          ++i;
        }
      }
    }
    if (i % 50 != 1) {
      bulkWriteOperation.execute();
      log.info("Services: {} updated", i);
    }
    log.info("Migrating Empty Service Names completed");
  }
}
