package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static java.util.Arrays.asList;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;

@Slf4j
public class DropOldCollectionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    for (String collection : asList("appdynamicsMetricDefinitions", "appdynamicsMetrics", "completedMetricsSummary",
             "containerDeploymentQueue", "entityUpdateListQueue", "history", "metricSummary", "newRelicMetricNames",
             "splunkAnalysisRecords", "splunkLogs")) {
      try {
        wingsPersistence.getCollection(DEFAULT_STORE, collection).drop();
      } catch (RuntimeException ex) {
        logger.error("Drop collection {} error", collection, ex);
      }
    }
  }
}