package io.harness.migrations.all;

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
