package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.DataStorageMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.WorkflowExecutionBaselineServiceImpl;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.DataStoreService;

public class NewRelicMetricDataBaselineMigration implements Migration {
  private static Logger logger = LoggerFactory.getLogger(NewRelicMetricDataBaselineMigration.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public void migrate() {
    if (mainConfiguration.getExecutionLogsStorageMode() != DataStorageMode.GOOGLE_CLOUD_DATA_STORE) {
      logger.info("google data store not enabled, returning....");
      return;
    }

    try (HIterator<WorkflowExecutionBaseline> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecutionBaseline.class).fetch())) {
      while (iterator.hasNext()) {
        WorkflowExecutionBaseline baseline = iterator.next();
        logger.info("marking baseline for {} ", baseline);
        PageRequest<NewRelicMetricDataRecord> pageRequest =
            aPageRequest().addFilter("workflowExecutionId", Operator.EQ, baseline.getWorkflowExecutionId()).build();
        PageResponse<NewRelicMetricDataRecord> metricDataRecords =
            dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);

        logger.info("num Of records: ", metricDataRecords.size());
        if (!metricDataRecords.isEmpty()) {
          metricDataRecords.forEach(
              dataRecord -> dataRecord.setValidUntil(WorkflowExecutionBaselineServiceImpl.BASELINE_TTL));

          dataStoreService.save(NewRelicMetricDataRecord.class, metricDataRecords);
        }
      }
    }

    logger.info("migration done...");
  }
}