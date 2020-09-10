package migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.security.encryption.EncryptionType.VAULT;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.beans.VaultConfig;
import software.wings.beans.VaultConfig.VaultConfigKeys;
import software.wings.dl.WingsPersistence;

@OwnedBy(PL)
@Slf4j
public class VaultAppRoleRenewalMigration implements Migration {
  private WingsPersistence wingsPersistence;
  private static final long DEFAULT_RENEWAL_INTERVAL = 15;

  @Inject
  public VaultAppRoleRenewalMigration(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    logger.info("Migrate vault app role renewal interval");
    try (HIterator<VaultConfig> iterator = new HIterator<>(wingsPersistence.createQuery(VaultConfig.class)
                                                               .filter(SecretManagerConfigKeys.encryptionType, VAULT)
                                                               .filter(VaultConfigKeys.renewalInterval, 0)
                                                               .field(VaultConfigKeys.appRoleId)
                                                               .exists()
                                                               .fetch())) {
      while (iterator.hasNext()) {
        VaultConfig vaultConfig = iterator.next();
        try {
          logger.info("Processing vault {}", vaultConfig.getUuid());
          wingsPersistence.updateField(
              VaultConfig.class, vaultConfig.getUuid(), VaultConfigKeys.renewalInterval, DEFAULT_RENEWAL_INTERVAL);
          logger.info("Updated vault config id {}", vaultConfig.getUuid());
        } catch (Exception e) {
          logger.error("Exception while updating vault config id: {}", vaultConfig.getUuid(), e);
        }
      }
    }
    logger.info("Migration completed for vault config renewal interval");
  }
}
