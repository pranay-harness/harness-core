/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppKeywordsMigration implements Migration {
  public static final int BATCH_SIZE = 50;

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(Application.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int i = 1;
    try (HIterator<Application> apps = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (apps.hasNext()) {
        Application application = apps.next();
        if (i % BATCH_SIZE == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("Applications: {} updated", i);
        }
        ++i;
        Set<String> keywords = application.generateKeywords();
        bulkWriteOperation
            .find(
                wingsPersistence.createQuery(Service.class).filter(Service.ID, application.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("keywords", trimmedLowercaseSet(keywords))));
      }
    }
    if (i % BATCH_SIZE != 1) {
      bulkWriteOperation.execute();
    }
  }
}
