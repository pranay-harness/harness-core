package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Slf4j
public class AddValidUntilToStateExecutionInstance implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(StateExecutionInstance.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                 .field("validUntil")
                                 .doesNotExist()
                                 .project(StateExecutionInstanceKeys.createdAt, true)
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
                      .filter(StateExecutionInstanceKeys.uuid, stateExecutionInstance.getUuid())
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
