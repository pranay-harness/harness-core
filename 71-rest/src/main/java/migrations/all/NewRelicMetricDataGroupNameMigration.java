package migrations.all;

import static software.wings.beans.Base.ID_KEY;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

/**
 * Created by rsingh on 3/26/18.
 */
@Slf4j
public class NewRelicMetricDataGroupNameMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(NewRelicMetricDataRecord.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int updated = 0;
    int batched = 0;
    try (HIterator<NewRelicMetricDataRecord> dataRecords = new HIterator<>(
             wingsPersistence.createQuery(NewRelicMetricDataRecord.class).field("groupName").doesNotExist().fetch())) {
      while (dataRecords.hasNext()) {
        final NewRelicMetricDataRecord metricDataRecord = dataRecords.next();

        bulkWriteOperation
            .find(wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                      .filter(ID_KEY, metricDataRecord.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("groupName", DEFAULT_GROUP_NAME)));
        updated++;
        batched++;

        if (updated != 0 && updated % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          batched = 0;
          logger.info("updated: " + updated);
        }
      }

      if (batched != 0) {
        bulkWriteOperation.execute();
        logger.info("updated: " + updated);
      }
    }

    logger.info("Complete. Updated " + updated + " records.");
  }
}
