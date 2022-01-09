/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;

import software.wings.beans.AuthToken;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class AuthTokenTtlMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(AuthToken.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    bulkWriteOperation.find(wingsPersistence.createQuery(AuthToken.class).field("ttl").doesNotExist().getQueryObject())
        .update(new BasicDBObject(
            "$set", new BasicDBObject("ttl", new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))));
    bulkWriteOperation.execute();
  }
}
