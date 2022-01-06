/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static software.wings.beans.Base.ID_KEY2;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;

/**
 * Created by rsingh on 3/26/18.
 */
public abstract class AddFieldMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(getCollectionClass());
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    DBCursor dataRecords = collection.find();

    getLogger().info("will go through " + dataRecords.size() + " records");

    int updated = 0;
    int batched = 0;
    while (dataRecords.hasNext()) {
      DBObject record = dataRecords.next();

      String uuId = (String) record.get("_id");
      bulkWriteOperation.find(wingsPersistence.createQuery(getCollectionClass()).filter(ID_KEY2, uuId).getQueryObject())
          .updateOne(new BasicDBObject("$set", new BasicDBObject(getFieldName(), getFieldValue(record))));
      updated++;
      batched++;

      if (updated != 0 && updated % 1000 == 0) {
        bulkWriteOperation.execute();
        bulkWriteOperation = collection.initializeUnorderedBulkOperation();
        batched = 0;
        getLogger().info("updated: " + updated);
      }
    }

    if (batched != 0) {
      bulkWriteOperation.execute();
      getLogger().info("updated: " + updated);
    }

    getLogger().info("Complete. Updated " + updated + " records.");
  }
  protected abstract Logger getLogger();

  protected abstract String getCollectionName();

  protected abstract Class getCollectionClass();

  protected abstract String getFieldName();

  protected abstract Object getFieldValue(DBObject existingRecord);
}
