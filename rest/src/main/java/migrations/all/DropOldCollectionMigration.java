package migrations.all;

import static java.util.Arrays.asList;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

public class DropOldCollectionMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DropOldCollectionMigration.class);
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    for (String collection : asList("appdynamicsMetricDefinitions", "appdynamicsMetrics", "completedMetricsSummary",
             "containerDeploymentQueue", "entityUpdateListQueue", "history", "metricSummary", "newRelicMetricNames",
             "splunkAnalysisRecords", "splunkLogs")) {
      try {
        wingsPersistence.getCollection(collection).drop();
      } catch (RuntimeException ex) {
        logger.error("Drop collection {} error", collection, ex);
      }
    }
  }
}