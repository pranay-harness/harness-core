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
import software.wings.beans.DelegateTask;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToDelegateTask implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToDelegateTask.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "delegateTasks");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<DelegateTask> delegateTasks = new HIterator<>(wingsPersistence.createQuery(DelegateTask.class)
                                                                     .field("validUntil")
                                                                     .doesNotExist()
                                                                     .project("createdAt", true)
                                                                     .fetch())) {
      while (delegateTasks.hasNext()) {
        final DelegateTask delegateTask = delegateTasks.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(delegateTask.getCreatedAt()).atZone(ZoneOffset.UTC).plusDays(7);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("DelegateTasks: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(DelegateTask.class)
                      .filter(DelegateTask.ID_KEY, delegateTask.getUuid())
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
