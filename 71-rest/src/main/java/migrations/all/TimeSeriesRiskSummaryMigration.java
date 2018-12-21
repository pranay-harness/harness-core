package migrations.all;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

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
  private static final Logger logger = LoggerFactory.getLogger(CleanUpDatadogCallLogMigration.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    // Fetch the list of current valid cvConfigurations
    List<CVConfiguration> cvConfigurations =
        wingsPersistence.createQuery(CVConfiguration.class).filter("enabled24x7", true).asList();

    for (CVConfiguration config : cvConfigurations) {
      PageRequest<TimeSeriesMLAnalysisRecord> recordPageRequest =
          PageRequestBuilder.aPageRequest()
              .withLimit("999")
              .addFilter("cvConfigId", Operator.EQ, config.getUuid())
              .withOffset("0")
              .build();

      PageResponse<TimeSeriesMLAnalysisRecord> response =
          wingsPersistence.query(TimeSeriesMLAnalysisRecord.class, recordPageRequest);

      while (!response.isEmpty()) {
        List<TimeSeriesMLAnalysisRecord> records = response.getResponse();
        saveRiskSummaries(records);
        recordPageRequest.setOffset(response.getOffset());
        response = wingsPersistence.query(TimeSeriesMLAnalysisRecord.class, recordPageRequest);
      }
      sleep(ofMillis(1000));
    }
  }

  private void saveRiskSummaries(List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords) {
    List<TimeSeriesRiskSummary> riskSummaries = new ArrayList<>();
    timeSeriesMLAnalysisRecords.forEach(mlAnalysisResponse -> {
      TimeSeriesRiskSummary riskSummary = TimeSeriesRiskSummary.builder()
                                              .analysisMinute(mlAnalysisResponse.getAnalysisMinute())
                                              .cvConfigId(mlAnalysisResponse.getCvConfigId())
                                              .build();

      riskSummary.setAppId(mlAnalysisResponse.getAppId());
      TreeBasedTable<String, String, Integer> risks = TreeBasedTable.create();
      TreeBasedTable<String, String, Integer> longTermPatterns = TreeBasedTable.create();
      for (TimeSeriesMLTxnSummary txnSummary : mlAnalysisResponse.getTransactions().values()) {
        for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
          if (mlMetricSummary.getResults() != null) {
            risks.put(txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getMax_risk());
            longTermPatterns.put(
                txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getLong_term_pattern());
          }
        }
      }

      riskSummary.setTxnMetricRisk(risks.rowMap());
      riskSummary.setTxnMetricLongTermPattern(longTermPatterns.rowMap());
      riskSummary.compressMaps();
      riskSummaries.add(riskSummary);
    });

    wingsPersistence.save(riskSummaries);
  }
}
