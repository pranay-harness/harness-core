package migrations.all;

import static io.harness.security.encryption.EncryptionType.VAULT;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.beans.VaultConfig;
import software.wings.beans.VaultConfig.VaultConfigKeys;
import software.wings.dl.WingsPersistence;

import java.time.Duration;

@Slf4j
public class VaultConfigRenewalIntervalMigration implements Migration {
  private WingsPersistence wingsPersistence;

  @Inject
  public VaultConfigRenewalIntervalMigration(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    logger.info("Migrate vault configurations renewal interval");
    try (HIterator<SecretManagerConfig> iterator =
             new HIterator<>(wingsPersistence.createQuery(SecretManagerConfig.class)
                                 .filter(SecretManagerConfigKeys.encryptionType, VAULT)
                                 .fetch())) {
      while (iterator.hasNext()) {
        VaultConfig vaultConfig = (VaultConfig) iterator.next();
        try {
          logger.info("Processing vault {}", vaultConfig.getUuid());
          long renewalInterval = Duration.ofHours(vaultConfig.getRenewIntervalHours()).toMinutes();
          wingsPersistence.updateField(
              SecretManagerConfig.class, vaultConfig.getUuid(), VaultConfigKeys.renewalInterval, renewalInterval);
          logger.info("Updated vault config id {}", vaultConfig.getUuid());
        } catch (Exception e) {
          logger.error("Exception while updating vault config id: {}", vaultConfig.getUuid(), e);
        }
      }
    }
    logger.info("Migration completed for vault config renewal interval");
  }
}
