package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;

@Slf4j
public class InvalidCVConfigDeletionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EnvironmentService environmentService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void migrate() {
    logger.info("Staring InvalidCVConfigDeletionMigration");
    int deleted = 0;
    try (HIterator<CVConfiguration> iterator =
             new HIterator<>(wingsPersistence.createQuery(CVConfiguration.class, excludeAuthority).fetch())) {
      while (iterator.hasNext()) {
        try {
          final CVConfiguration cvConfiguration = iterator.next();
          Service service = serviceResourceService.get(cvConfiguration.getServiceId());
          Environment environment = environmentService.get(cvConfiguration.getAppId(), cvConfiguration.getEnvId());

          if (service == null || environment == null) {
            logger.info("for {} deleting {} with id {}, service: {} environment: {}", cvConfiguration.getAccountId(),
                cvConfiguration.getName(), cvConfiguration.getUuid(), service, environment);
            cvConfigurationService.deleteConfiguration(
                cvConfiguration.getAccountId(), cvConfiguration.getAppId(), cvConfiguration.getUuid());
            deleted++;
          }
          sleep(ofMillis(500));
        } catch (Exception e) {
          logger.info("Error while running migration", e);
        }
      }
    }
    logger.info("Complete. deleted " + deleted + " records.");
  }
}
