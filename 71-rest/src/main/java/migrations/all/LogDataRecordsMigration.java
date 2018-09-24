package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Base.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogDataRecord;

/**
 * Created by rsingh on 3/26/18.
 */
public class LogDataRecordsMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(LogDataRecordsMigration.class);

  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection("logDataRecords");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    DBCursor logDataRecords = wingsPersistence.getCollection("logDataRecords").find();

    logger.info("will go through " + logDataRecords.size() + " records");

    int updated = 0;
    int batched = 0;
    while (logDataRecords.hasNext()) {
      DBObject next = logDataRecords.next();

      String uuId = (String) next.get("_id");
      String appId = (String) next.get("applicationId");
      if (isEmpty(appId)) {
        continue;
      }
      bulkWriteOperation.find(wingsPersistence.createQuery(LogDataRecord.class).filter(ID_KEY, uuId).getQueryObject())
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
