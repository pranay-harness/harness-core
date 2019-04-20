package migrations.all;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
@Slf4j
public class TimeSeriesMLAnalysisDeleteUncompressedMigration implements Migration {
  private static final int BATCH_SIZE = 50;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(TimeSeriesMLAnalysisRecord.class, ReadPref.NORMAL);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int batched = 0;
    int processed = 0;
    try (HIterator<TimeSeriesMLAnalysisRecord> iterator =
             new HIterator<>(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class).fetch())) {
      while (iterator.hasNext()) {
        TimeSeriesMLAnalysisRecord mlAnalysisRecord = iterator.next();
        processed++;
        logger.info("saving " + mlAnalysisRecord.getUuid());
        bulkWriteOperation
            .find(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                      .filter(TimeSeriesMLAnalysisRecord.ID_KEY, mlAnalysisRecord.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$unset", new BasicDBObject("transactions", "")));
        batched++;

        if (processed != 0 && batched % BATCH_SIZE == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          batched = 0;
          logger.info("updated: " + processed);
        }
      }
      if (batched != 0) {
        bulkWriteOperation.execute();
        logger.info("updated: " + processed);
      }
      logger.info("processed " + processed);
    }
  }
}