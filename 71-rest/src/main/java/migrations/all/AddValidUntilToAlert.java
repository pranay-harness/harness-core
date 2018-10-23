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
import software.wings.beans.alert.Alert;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class AddValidUntilToAlert implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(AddValidUntilToAlert.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "alerts");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<Alert> alerts = new HIterator<>(wingsPersistence.createQuery(Alert.class)
                                                       .field("validUntil")
                                                       .doesNotExist()
                                                       .project("closedAt", true)
                                                       .fetch())) {
      while (alerts.hasNext()) {
        final Alert alert = alerts.next();
        final ZonedDateTime zonedDateTime = alert.getClosedAt() == 0
            ? OffsetDateTime.now().plusMonths(1).toZonedDateTime()
            : Instant.ofEpochMilli(alert.getClosedAt()).atZone(ZoneOffset.UTC).plusDays(7);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("Alerts: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Alert.class).filter(Alert.ID_KEY, alert.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject(
                "$set", new BasicDBObject("validUntil", java.util.Date.from(zonedDateTime.toInstant()))));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
