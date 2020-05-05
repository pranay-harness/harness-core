package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.Integer.max;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
@Slf4j
public class TimeSeriesMLAnalysisCompressionSaveMigration implements Migration {
  private static final int BATCH_SIZE = 50;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection = wingsPersistence.getCollection(TimeSeriesMLAnalysisRecord.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int batched = 0;
    int processed = 0;
    try (HIterator<TimeSeriesMLAnalysisRecord> iterator =
             new HIterator<>(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class).fetch())) {
      while (iterator.hasNext()) {
        TimeSeriesMLAnalysisRecord mlAnalysisRecord = iterator.next();
        int aggregatedRisk = aggregateRiskOfRecord(mlAnalysisRecord);
        mlAnalysisRecord.bundleAsJosnAndCompress();
        processed++;
        logger.info("saving " + mlAnalysisRecord.getUuid());
        bulkWriteOperation
            .find(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                      .filter(TimeSeriesMLAnalysisRecord.ID_KEY, mlAnalysisRecord.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$set",
                new BasicDBObject("transactionsCompressedJson", mlAnalysisRecord.getTransactionsCompressedJson())));
        bulkWriteOperation
            .find(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                      .filter(TimeSeriesMLAnalysisRecord.ID_KEY, mlAnalysisRecord.getUuid())
                      .getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject("aggregatedRisk", aggregatedRisk)));
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

  private int aggregateRiskOfRecord(TimeSeriesMLAnalysisRecord mlAnalysisRecord) {
    mlAnalysisRecord.decompress();
    int aggregatedRisk = -1;
    if (isEmpty(mlAnalysisRecord.getTransactions())) {
      return aggregatedRisk;
    }
    for (TimeSeriesMLTxnSummary txn : mlAnalysisRecord.getTransactions().values()) {
      if (txn != null && isNotEmpty(txn.getMetrics())) {
        for (TimeSeriesMLMetricSummary metric : txn.getMetrics().values()) {
          if (metric != null) {
            aggregatedRisk = max(aggregatedRisk, metric.getMax_risk());
          }
        }
      }
    }
    return aggregatedRisk;
  }
}