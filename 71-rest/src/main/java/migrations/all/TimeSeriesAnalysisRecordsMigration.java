package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Base.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;

/**
 * Created by rsingh on 3/26/18.
 */
@Slf4j
public class TimeSeriesAnalysisRecordsMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(TimeSeriesMLAnalysisRecord.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    DBCursor timeSeriesAnalysisRecords = collection.find();

    logger.info("will go through " + timeSeriesAnalysisRecords.size() + " records");

    int updated = 0;
    int batched = 0;
    while (timeSeriesAnalysisRecords.hasNext()) {
      DBObject next = timeSeriesAnalysisRecords.next();

      String uuId = (String) next.get("_id");
      String appId = (String) next.get("applicationId");
      if (isEmpty(appId)) {
        continue;
      }
      bulkWriteOperation
          .find(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class).filter(ID_KEY, uuId).getQueryObject())
          .updateOne(new BasicDBObject("$set", new BasicDBObject("appId", appId)));
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

    logger.info("Complete. Updated " + updated + " records.");
  }
}
