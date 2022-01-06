/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;

import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("deprecation")
public class UpdateBitBucketTriggers implements Migration {
  @Inject WingsPersistence wingsPersistence;

  private static int BATCH_SIZE = 50;
  @Override
  public void migrate() {
    migratePushEvents();
    migratePullRequestEvents();
  }

  private void migratePushEvents() {
    final DBCollection collection = wingsPersistence.getCollection(Trigger.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int processedDocsCount = 1;

    BasicDBObject bitbucketFilter = new BasicDBObject("condition.webhookSource", "BITBUCKET");
    BasicDBObject pushRequestFilter = new BasicDBObject("condition.eventTypes", "PUSH");

    DBObject bitBucketPushRequestFilter = new BasicDBObject();
    bitBucketPushRequestFilter.put("$and", Arrays.asList(bitbucketFilter, pushRequestFilter));

    DBCursor triggers = wingsPersistence.getCollection(Trigger.class).find(bitBucketPushRequestFilter);

    try {
      BasicDBList basicDBList = new BasicDBList();
      basicDBList.add("ALL");

      BasicDBList eventList = new BasicDBList();
      eventList.add("REPO");
      while (triggers.hasNext()) {
        DBObject object = triggers.next();
        String uuId = (String) object.get(Trigger.ID_KEY2);
        bulkWriteOperation
            .find(wingsPersistence.createQuery(Trigger.class).filter(Trigger.ID_KEY2, uuId).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("condition.bitBucketEvents", basicDBList)));

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Trigger.class).filter(Trigger.ID_KEY2, uuId).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("condition.eventTypes", eventList)));

        if (processedDocsCount % BATCH_SIZE == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("triggers: {} updated", processedDocsCount);
        }
        processedDocsCount = processedDocsCount + 1;
      }

      if (processedDocsCount % BATCH_SIZE != 1) {
        bulkWriteOperation.execute();
      }

    } finally {
      triggers.close();
    }
  }

  private void migratePullRequestEvents() {
    final DBCollection collection = wingsPersistence.getCollection(Trigger.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int processedDocsCount = 1;

    BasicDBObject bitbucketFilter = new BasicDBObject("condition.webhookSource", "BITBUCKET");
    BasicDBObject pullRequestFilter = new BasicDBObject("condition.eventTypes", "PULL_REQUEST");

    DBObject bitBucketPullRequestFilter = new BasicDBObject();
    bitBucketPullRequestFilter.put("$and", Arrays.asList(bitbucketFilter, pullRequestFilter));

    DBCursor triggers = wingsPersistence.getCollection(Trigger.class).find(bitBucketPullRequestFilter);

    try {
      BasicDBList basicDBList = new BasicDBList();
      basicDBList.add("ALL");

      while (triggers.hasNext()) {
        DBObject object = triggers.next();
        String uuId = (String) object.get(Trigger.ID_KEY2);
        bulkWriteOperation
            .find(wingsPersistence.createQuery(Trigger.class).filter(Trigger.ID_KEY2, uuId).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("condition.bitBucketEvents", basicDBList)));

        if (processedDocsCount % BATCH_SIZE == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("triggers: {} updated", processedDocsCount);
        }
        processedDocsCount = processedDocsCount + 1;
      }

      if (processedDocsCount % BATCH_SIZE != 1) {
        bulkWriteOperation.execute();
      }

    } finally {
      triggers.close();
    }
  }
}
