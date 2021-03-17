package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._390_DB_MIGRATION)
public class TimeSeriesThresholdsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void migrate() {
    int updated = 0;
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).fetch())) {
      while (iterator.hasNext()) {
        CVConfiguration configuration = iterator.next();
        cvConfigurationService.updateConfiguration(configuration, configuration.getAppId());
        updated++;
      }
    }
    log.info("Complete. updated " + updated + " records.");
  }
}
