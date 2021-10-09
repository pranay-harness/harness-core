package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteInvalidServiceGuardConfigs implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @Override
  public void migrate() {
    log.info("Delete invalid cv configurations");
    Set<String> configsToBeDeleted = new HashSet<>();
    try (HIterator<CVConfiguration> cvConfigurationHIterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class).fetch())) {
      while (cvConfigurationHIterator.hasNext()) {
        CVConfiguration cvConfiguration = cvConfigurationHIterator.next();
        SettingAttribute settingAttribute = settingsService.get(cvConfiguration.getConnectorId());
        if (settingAttribute == null) {
          log.info("Deleting cv configuration {}", cvConfiguration);
          configsToBeDeleted.add(cvConfiguration.getUuid());
        }
      }
    }
    wingsPersistence.delete(
        wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).field("_id").in(configsToBeDeleted));
    log.info("Finished deleting invalid cv configs");
  }
}
