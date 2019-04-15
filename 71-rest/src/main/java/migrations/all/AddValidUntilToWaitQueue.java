package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import io.harness.waiter.WaitQueue;
import io.harness.waiter.WaitQueue.WaitQueueKeys;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Slf4j
public class AddValidUntilToWaitQueue implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(WaitQueue.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<WaitQueue> waitQueues = new HIterator<>(wingsPersistence.createQuery(WaitQueue.class)
                                                               .field("validUntil")
                                                               .doesNotExist()
                                                               .project(WaitQueueKeys.createdAt, true)
                                                               .fetch())) {
      while (waitQueues.hasNext()) {
        final WaitQueue waitQueue = waitQueues.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(waitQueue.getCreatedAt()).atZone(ZoneOffset.UTC).plusMonths(1);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("logs: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Log.class).filter(Log.ID_KEY, waitQueue.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject("validUntil", java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
