package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.UuidAware.ID_KEY;
import static java.lang.Integer.max;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;

public class TimeSeriesMLAnalysisCompressionSaveMigration implements Migration {
  private static Logger logger = LoggerFactory.getLogger(TimeSeriesMLAnalysisCompressionSaveMigration.class);
  private static final int BATCH_SIZE = 50;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    final DBCollection collection =
        wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "timeSeriesAnalysisRecords");
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();
    int updated = 0;
    int batched = 0;
    int processed = 0;
    try (HIterator<TimeSeriesMLAnalysisRecord> iterator =
             new HIterator<>(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class).fetch())) {
      while (iterator.hasNext()) {
        TimeSeriesMLAnalysisRecord mlAnalysisRecord = iterator.next();
        int aggregatedRisk = aggregateRiskOfRecord(mlAnalysisRecord);
        mlAnalysisRecord.compressTransactions();
        processed++;
        if (isNotEmpty(mlAnalysisRecord.getTransactionsCompressedJson())) {
          logger.info("saving " + mlAnalysisRecord.getUuid());
          bulkWriteOperation
              .find(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                        .filter(ID_KEY, mlAnalysisRecord.getUuid())
                        .getQueryObject())
              .updateOne(new BasicDBObject("$set",
                  new BasicDBObject("transactionsCompressedJson", mlAnalysisRecord.getTransactionsCompressedJson()))
                             .append("aggregatedRisk", aggregatedRisk));
          updated++;
          batched++;
        } else {
          logger.info("ignoring " + mlAnalysisRecord.getUuid());
        }

        if (updated != 0 && batched % BATCH_SIZE == 0) {
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
      logger.info("processed " + processed);
    }
  }

  private int aggregateRiskOfRecord(TimeSeriesMLAnalysisRecord mlAnalysisRecord) {
    mlAnalysisRecord.decompressTransactions();
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