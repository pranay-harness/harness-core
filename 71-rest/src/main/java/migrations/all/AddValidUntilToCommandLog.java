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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToCommandLog implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToCommandLog.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "commandLogs");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<Log> logs = new HIterator<>(wingsPersistence.createQuery(Log.class)
                                                   .field("validUntil")
                                                   .doesNotExist()
                                                   .project("createdAt", true)
                                                   .fetch())) {
      while (logs.hasNext()) {
        final Log log = logs.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(log.getCreatedAt()).atZone(ZoneOffset.UTC).plusMonths(6);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("logs: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Log.class).filter(Log.ID_KEY, log.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject("validUntil", java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
