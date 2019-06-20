package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityKeys;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Slf4j
public class AddValidUntilToActivity implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(Activity.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<Activity> activities = new HIterator<>(wingsPersistence.createQuery(Activity.class)
                                                              .field(ActivityKeys.validUntil)
                                                              .doesNotExist()
                                                              .project(ActivityKeys.createdAt, true)
                                                              .fetch())) {
      while (activities.hasNext()) {
        final Activity activity = activities.next();
        final ZonedDateTime zonedDateTime =
            Instant.ofEpochMilli(activity.getCreatedAt()).atZone(ZoneOffset.UTC).plusMonths(6);

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("activities: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Activity.class)
                      .filter(ActivityKeys.uuid, activity.getUuid())
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
