package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._390_DB_MIGRATION)
public class DisableServiceGuardsWithDeletedConnectorsMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("starting migration");
    try (HIterator<CVConfiguration> cvConfigurations =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).fetch())) {
      while (cvConfigurations.hasNext()) {
        CVConfiguration cvConfiguration = cvConfigurations.next();
        final String connectorId = cvConfiguration.getConnectorId();
        final SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, connectorId);

        if (settingAttribute == null) {
          log.info("for {} in account {} the connector has been deleted. Disabling the service guard",
              cvConfiguration.getUuid(), cvConfiguration.getAccountId());
          wingsPersistence.updateField(
              CVConfiguration.class, cvConfiguration.getUuid(), CVConfigurationKeys.enabled24x7, Boolean.FALSE);
          sleep(ofMillis(100));
        }
      }
    }

    log.info("migration finished");
  }
}
