package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityKeys;
import software.wings.dl.WingsPersistence;

@Slf4j
public class RemoveServiceVariablesFromActivity implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(Activity.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 1;
    try (HIterator<Activity> activities = new HIterator<>(wingsPersistence.createQuery(Activity.class)
                                                              .field("serviceVariables")
                                                              .exists()
                                                              .project(ActivityKeys.appId, true)
                                                              .fetch())) {
      while (activities.hasNext()) {
        final Activity activity = activities.next();

        if (i % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("activities: {} updated", i);
        }
        ++i;

        bulkWriteOperation
            .find(wingsPersistence.createQuery(Activity.class)
                      .filter(ActivityKeys.appId, activity.getAppId())
                      .filter(ActivityKeys.uuid, activity.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$unset", new BasicDBObject("serviceVariables", "")));
      }
    }
    if (i % 1000 != 1) {
      bulkWriteOperation.execute();
    }
  }
}
