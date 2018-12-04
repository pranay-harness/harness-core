package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static software.wings.beans.Base.ID_KEY;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;

/**
 * Created by rsingh on 3/26/18.
 */
public class LogFeedbackRecordsMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(LogFeedbackRecordsMigration.class);

  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "logMlFeedbackRecords");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    DBCursor logFeedbackRecords = collection.find();

    logger.info("will go through " + logFeedbackRecords.size() + " records");

    int updated = 0;
    int batched = 0;
    while (logFeedbackRecords.hasNext()) {
      DBObject next = logFeedbackRecords.next();

      String uuId = (String) next.get("_id");
      String appId = (String) next.get("applicationId");
      if (isEmpty(appId)) {
        continue;
      }
      bulkWriteOperation
          .find(wingsPersistence.createQuery(LogMLFeedbackRecord.class).filter(ID_KEY, uuId).getQueryObject())
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
