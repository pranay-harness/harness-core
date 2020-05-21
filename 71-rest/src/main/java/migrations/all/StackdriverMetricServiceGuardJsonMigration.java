package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

@Slf4j
public class StackdriverMetricServiceGuardJsonMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Migrate stack driver metric service guard configurations");
    try (HIterator<CVConfiguration> cvConfigurationHIterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class)
                                 .filter("stateType", StateType.STACK_DRIVER.name())
                                 .fetch())) {
      while (cvConfigurationHIterator.hasNext()) {
        StackDriverMetricCVConfiguration cvConfiguration =
            (StackDriverMetricCVConfiguration) cvConfigurationHIterator.next();
        try {
          logger.info("Processing config id {}", cvConfiguration.getUuid());
          cvConfiguration.getMetricDefinitions().forEach(metricDefinition -> {
            if (metricDefinition.checkIfOldFilter()) {
              String updatedFilterJson =
                  StackDriverMetricDefinition.getUpdatedFilterJson(metricDefinition.getFilterJson());
              metricDefinition.setFilterJson(updatedFilterJson);
              metricDefinition.extractJson();
            }
          });
          wingsPersistence.save(cvConfiguration);
          logger.info("Saved config id {}", cvConfiguration.getUuid());
        } catch (Exception e) {
          logger.error("Exception while updating config id: {}", cvConfiguration.getUuid(), e);
        }
      }
    }
    logger.info("Migration completed for stack driver metric service guard configurations");
  }
}
