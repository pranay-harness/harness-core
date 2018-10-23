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
import software.wings.sm.StateExecutionInstance;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToStateExecutionInstance implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToStateExecutionInstance.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection =
        wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "stateExecutionInstances");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .field("validUntil")
                                 .doesNotExist()
                                 .project("createdAt", true)
                                 .fetch())) {
      while (stateExecutionInstances.hasNext()) {
        final StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(stateExecutionInstance.getCreatedAt()).atZone(ZoneOffset.UTC).plusMonths(6);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("StateExecutionInstance: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(StateExecutionInstance.class)
                      .filter(StateExecutionInstance.ID_KEY, stateExecutionInstance.getUuid())
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
