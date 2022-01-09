/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveServiceVariablesFromActivity implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(Activity.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<Activity> activities = new HIterator<>(wingsPersistence.createQuery(Activity.class)
                                                              .field("serviceVariables")
                                                              .exists()
                                                              .project(ActivityKeys.appId, true)
                                                              .fetch())) {
      while (activities.hasNext()) {
        final Activity activity = activities.next();

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("activities: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Activity.class)
                      .filter(ActivityKeys.appId, activity.getAppId())
                      .filter(ActivityKeys.uuid, activity.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$unset", new BasicDBObject("serviceVariables", "")));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
