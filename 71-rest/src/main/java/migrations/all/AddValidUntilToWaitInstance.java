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
import software.wings.dl.WingsPersistence;
import software.wings.waitnotify.WaitInstance;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToWaitInstance implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToWaitInstance.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "waitInstances");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<WaitInstance> waitInstances = new HIterator<>(wingsPersistence.createQuery(WaitInstance.class)
                                                                     .field("validUntil")
                                                                     .doesNotExist()
                                                                     .project("createdAt", true)
                                                                     .fetch())) {
      while (waitInstances.hasNext()) {
        final WaitInstance waitInstance = waitInstances.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(waitInstance.getCreatedAt()).atZone(ZoneOffset.UTC).plusMonths(1);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("WaitInstance: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(WaitInstance.class)
                      .filter(WaitInstance.ID_KEY, waitInstance.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject("validUntil", java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
