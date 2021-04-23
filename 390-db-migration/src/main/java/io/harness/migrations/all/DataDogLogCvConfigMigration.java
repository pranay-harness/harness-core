package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class DataDogLogCvConfigMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    UpdateOperations<CVConfiguration> updateOperations =
        wingsPersistence.createUpdateOperations(CVConfiguration.class)
            .disableValidation()
            .set("className", "software.wings.verification.datadog.DatadogLogCVConfiguration");
    Query<CVConfiguration> query = wingsPersistence.createQuery(CVConfiguration.class)
                                       .filter(CVConfigurationKeys.stateType, StateType.DATA_DOG_LOG);
    UpdateResults updateResults = wingsPersistence.update(query, updateOperations);
    log.info("Updated cvConfig Id with DATA_DOG_LOG: {} ", updateResults);
  }
}
