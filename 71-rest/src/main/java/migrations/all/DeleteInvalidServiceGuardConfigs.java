package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;
import software.wings.verification.CVConfiguration;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class DeleteInvalidServiceGuardConfigs implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @Override
  public void migrate() {
    logger.info("Delete invalid cv configurations");
    Set<String> configsToBeDeleted = new HashSet<>();
    try (HIterator<CVConfiguration> cvConfigurationHIterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class).fetch())) {
      while (cvConfigurationHIterator.hasNext()) {
        CVConfiguration cvConfiguration = cvConfigurationHIterator.next();
        SettingAttribute settingAttribute = settingsService.get(cvConfiguration.getConnectorId());
        if (settingAttribute == null) {
          logger.info("Deleting cv configuration {}", cvConfiguration);
          configsToBeDeleted.add(cvConfiguration.getUuid());
        }
      }
    }
    wingsPersistence.delete(
        wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).field("_id").in(configsToBeDeleted));
    logger.info("Finished deleting invalid cv configs");
  }
}
