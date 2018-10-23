package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;
import software.wings.waitnotify.WaitQueue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToWaitQueue implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToWaitQueue.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "waitQueues");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<WaitQueue> waitQueues = new HIterator<>(wingsPersistence.createQuery(WaitQueue.class)
                                                               .field("validUntil")
                                                               .doesNotExist()
                                                               .project("createdAt", true)
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
