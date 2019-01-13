package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.analysis.TimeSeriesRiskSummary;
import software.wings.verification.CVConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Praveen
 */
public class TimeSeriesRiskSummaryMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(TimeSeriesRiskSummaryMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  private int completedCount;

  @Override
  public void migrate() {
    // Fetch the list of current valid cvConfigurations
    List<CVConfiguration> cvConfigurations =
        wingsPersistence.createQuery(CVConfiguration.class).filter("enabled24x7", true).asList();

    logger.info("Starting TimeSeriesRiskSummaryMigration. Total CV Configs to migrate: {}", cvConfigurations.size());
    for (CVConfiguration config : cvConfigurations) {
      logger.info("Currently migrating cvConfig: {}", config.getUuid());
      PageRequest<TimeSeriesMLAnalysisRecord> recordPageRequest =
          PageRequestBuilder.aPageRequest()
              .withLimit("999")
              .addFilter("cvConfigId", Operator.EQ, config.getUuid())
              .withOffset("0")
              .build();

      PageResponse<TimeSeriesMLAnalysisRecord> response =
          wingsPersistence.query(TimeSeriesMLAnalysisRecord.class, recordPageRequest, excludeAuthority);

      logger.info("The total number of records for cvConfigId {} is {}", config.getUuid(), response.getTotal());
      int previousOffSet = 0;
      while (!response.isEmpty()) {
        List<TimeSeriesMLAnalysisRecord> records = response.getResponse();
        logger.info("Currently Migrating for cvConfigId {} and batchsize {}", config.getUuid(), records.size());
        saveRiskSummaries(records);
        previousOffSet += response.size();
        recordPageRequest.setOffset(String.valueOf(previousOffSet));
        response = wingsPersistence.query(TimeSeriesMLAnalysisRecord.class, recordPageRequest, excludeAuthority);
      }
      sleep(ofMillis(1000));
    }
    logger.info("Completed TimeSeriesRiskSummaryMigration after migrating {} records", completedCount);
  }

  private void saveRiskSummaries(List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords) {
    List<TimeSeriesRiskSummary> riskSummaries = new ArrayList<>();

    timeSeriesMLAnalysisRecords.forEach(mlAnalysisResponse -> {
      logger.info("In TimeSeriesRiskSummaryMigration, processing record for config {} and minute {}",
          mlAnalysisResponse.getCvConfigId(), mlAnalysisResponse.getAnalysisMinute());
      mlAnalysisResponse.decompressTransactions();
      TimeSeriesRiskSummary riskSummary = TimeSeriesRiskSummary.builder()
                                              .analysisMinute(mlAnalysisResponse.getAnalysisMinute())
                                              .cvConfigId(mlAnalysisResponse.getCvConfigId())
                                              .build();

      riskSummary.setAppId(mlAnalysisResponse.getAppId());
      TreeBasedTable<String, String, Integer> risks = TreeBasedTable.create();
      TreeBasedTable<String, String, Integer> longTermPatterns = TreeBasedTable.create();
      if (isNotEmpty(mlAnalysisResponse.getTransactions())) {
        for (TimeSeriesMLTxnSummary txnSummary : mlAnalysisResponse.getTransactions().values()) {
          if (isNotEmpty(txnSummary.getMetrics())) {
            for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
              if (mlMetricSummary.getResults() != null) {
                risks.put(txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getMax_risk());
                longTermPatterns.put(
                    txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getLong_term_pattern());
              }
            }
          }
        }
      }

      riskSummary.setTxnMetricRisk(risks.rowMap());
      riskSummary.setTxnMetricLongTermPattern(longTermPatterns.rowMap());
      logger.info("Done creating the riskSummary for config {} and minute {}", mlAnalysisResponse.getCvConfigId(),
          mlAnalysisResponse.getAnalysisMinute());
      riskSummary.compressMaps();
      riskSummaries.add(riskSummary);
    });

    try {
      logger.info("Saving {} records of riskSummary", riskSummaries.size());
      if (isNotEmpty(riskSummaries)) {
        wingsPersistence.save(riskSummaries);
      }
    } catch (DuplicateKeyException ex) {
      logger.info("Swallowing duplicate key exception during migration.");
    }
    completedCount += timeSeriesMLAnalysisRecords.size();
    logger.info("So far, Completed Migrating {} records", completedCount);
  }
}
