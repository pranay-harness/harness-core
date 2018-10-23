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
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToDelegateConnectionResults implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToDelegateConnectionResults.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection =
        wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "delegateConnectionResults");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<DelegateConnectionResult> activities =
             new HIterator<>(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                 .field("validUntil")
                                 .doesNotExist()
                                 .project("createdAt", true)
                                 .fetch())) {
      while (activities.hasNext()) {
        final DelegateConnectionResult delegateConnectionResult = activities.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(delegateConnectionResult.getCreatedAt()).atZone(ZoneOffset.UTC).plusHours(6);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("delegateConnectionResults: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(DelegateConnectionResult.class)
                      .filter(DelegateConnectionResult.ID_KEY, delegateConnectionResult.getUuid())
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
