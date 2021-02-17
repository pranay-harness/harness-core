package migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.security.encryption.EncryptionType.VAULT;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.BaseVaultConfig.BaseVaultConfigKeys;
import software.wings.beans.VaultConfig;
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
    log.info("Migrate vault app role renewal interval");
    try (HIterator<VaultConfig> iterator = new HIterator<>(wingsPersistence.createQuery(VaultConfig.class)
                                                               .filter(SecretManagerConfigKeys.encryptionType, VAULT)
                                                               .filter(BaseVaultConfigKeys.renewalInterval, 0)
                                                               .field(BaseVaultConfigKeys.appRoleId)
                                                               .exists()
                                                               .fetch())) {
      while (iterator.hasNext()) {
        VaultConfig vaultConfig = iterator.next();
        try {
          log.info("Processing vault {}", vaultConfig.getUuid());
          wingsPersistence.updateField(
              VaultConfig.class, vaultConfig.getUuid(), BaseVaultConfigKeys.renewalInterval, DEFAULT_RENEWAL_INTERVAL);
          log.info("Updated vault config id {}", vaultConfig.getUuid());
        } catch (Exception e) {
          log.error("Exception while updating vault config id: {}", vaultConfig.getUuid(), e);
        }
      }
    }
    log.info("Migration completed for vault config renewal interval");
  }
}
