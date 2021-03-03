package migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.security.encryption.EncryptionType.VAULT;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.BaseVaultConfig.BaseVaultConfigKeys;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;

import java.time.Duration;


@OwnedBy(PL)
@Slf4j
public class VaultConfigRenewalIntervalMigration implements Migration {
  private WingsPersistence wingsPersistence;

  @Inject
  public VaultConfigRenewalIntervalMigration(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    log.info("Migrate vault configurations renewal interval");
    try (HIterator<SecretManagerConfig> iterator =
             new HIterator<>(wingsPersistence.createQuery(SecretManagerConfig.class)
                                 .filter(SecretManagerConfigKeys.encryptionType, VAULT)
                                 .fetch())) {
      while (iterator.hasNext()) {
        VaultConfig vaultConfig = (VaultConfig) iterator.next();
        try {
          log.info("Processing vault {}", vaultConfig.getUuid());
          long renewalInterval = Duration.ofHours(vaultConfig.getRenewIntervalHours()).toMinutes();
          wingsPersistence.updateField(
              SecretManagerConfig.class, vaultConfig.getUuid(), BaseVaultConfigKeys.renewalInterval, renewalInterval);
          log.info("Updated vault config id {}", vaultConfig.getUuid());
        } catch (Exception e) {
          log.error("Exception while updating vault config id: {}", vaultConfig.getUuid(), e);
        }
      }
    }
    log.info("Migration completed for vault config renewal interval");
  }
}
