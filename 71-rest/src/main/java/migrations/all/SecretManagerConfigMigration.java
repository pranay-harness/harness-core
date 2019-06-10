package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AwsSecretsManagerLegacyConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.KmsLegacyConfig;
import software.wings.beans.VaultConfig;
import software.wings.beans.VaultLegacyConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.SecretManagerConfigService;

/**
 * @author marklu on 2019-06-05
 */
@Slf4j
public class SecretManagerConfigMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManagerConfigService secretManagerConfigService;

  @Override
  public void migrate() {
    try (HIterator<KmsLegacyConfig> iterator =
             new HIterator<>(wingsPersistence.createQuery(KmsLegacyConfig.class, excludeAuthority).fetch())) {
      logger.info("Migrating KMS configs into the common secret manager collection...");
      int count = 0;
      while (iterator.hasNext()) {
        KmsLegacyConfig legacyConfig = iterator.next();
        KmsConfig migratedConfig = KmsConfig.builder()
                                       .accessKey(legacyConfig.getAccessKey())
                                       .secretKey(legacyConfig.getSecretKey())
                                       .kmsArn(legacyConfig.getKmsArn())
                                       .name(legacyConfig.getName())
                                       .region(legacyConfig.getRegion())
                                       .build();
        migratedConfig.setUuid(legacyConfig.getUuid());
        migratedConfig.setAccountId(legacyConfig.getAccountId());
        migratedConfig.setDefault(legacyConfig.isDefault());
        migratedConfig.setCreatedAt(legacyConfig.getCreatedAt());
        migratedConfig.setCreatedBy(legacyConfig.getCreatedBy());
        migratedConfig.setLastUpdatedAt(legacyConfig.getLastUpdatedAt());
        migratedConfig.setLastUpdatedBy(legacyConfig.getLastUpdatedBy());

        secretManagerConfigService.save(migratedConfig);
        count++;
      }
      logger.info("{} KMS config records have been migrated into the common secret manager collection.", count);
    }

    try (HIterator<AwsSecretsManagerLegacyConfig> iterator = new HIterator<>(
             wingsPersistence.createQuery(AwsSecretsManagerLegacyConfig.class, excludeAuthority).fetch())) {
      logger.info("Migrating AWS Secrets Manager configs into the common secret manager collection...");
      int count = 0;
      while (iterator.hasNext()) {
        AwsSecretsManagerLegacyConfig legacyConfig = iterator.next();
        AwsSecretsManagerConfig migratedConfig = AwsSecretsManagerConfig.builder()
                                                     .name(legacyConfig.getName())
                                                     .accessKey(legacyConfig.getAccessKey())
                                                     .secretKey(legacyConfig.getSecretKey())
                                                     .secretNamePrefix(legacyConfig.getSecretNamePrefix())
                                                     .region(legacyConfig.getRegion())
                                                     .build();
        migratedConfig.setUuid(legacyConfig.getUuid());
        migratedConfig.setAccountId(legacyConfig.getAccountId());
        migratedConfig.setDefault(legacyConfig.isDefault());
        migratedConfig.setCreatedAt(legacyConfig.getCreatedAt());
        migratedConfig.setCreatedBy(legacyConfig.getCreatedBy());
        migratedConfig.setLastUpdatedAt(legacyConfig.getLastUpdatedAt());
        migratedConfig.setLastUpdatedBy(legacyConfig.getLastUpdatedBy());

        secretManagerConfigService.save(migratedConfig);
        count++;
      }
      logger.info(
          "{} AWS Secrets Manager config records have been migrated into the common secret manager collection.", count);
    }

    try (HIterator<VaultLegacyConfig> iterator =
             new HIterator<>(wingsPersistence.createQuery(VaultLegacyConfig.class, excludeAuthority).fetch())) {
      logger.info("Migrating VAULT configs into the common secret manager collection...");
      int count = 0;
      while (iterator.hasNext()) {
        VaultLegacyConfig legacyConfig = iterator.next();
        VaultConfig migratedConfig = VaultConfig.builder()
                                         .name(legacyConfig.getName())
                                         .vaultUrl(legacyConfig.getVaultUrl())
                                         .authToken(legacyConfig.getAuthToken())
                                         .appRoleId(legacyConfig.getAppRoleId())
                                         .secretId(legacyConfig.getSecretId())
                                         .basePath(legacyConfig.getBasePath())
                                         .secretEngineVersion(legacyConfig.getSecretEngineVersion())
                                         .build();
        migratedConfig.setAccountId(legacyConfig.getAccountId());
        migratedConfig.setDefault(legacyConfig.isDefault());
        migratedConfig.setUuid(legacyConfig.getUuid());
        migratedConfig.setCreatedAt(legacyConfig.getCreatedAt());
        migratedConfig.setCreatedBy(legacyConfig.getCreatedBy());
        migratedConfig.setLastUpdatedAt(legacyConfig.getLastUpdatedAt());
        migratedConfig.setLastUpdatedBy(legacyConfig.getLastUpdatedBy());

        secretManagerConfigService.save(migratedConfig);
        count++;
      }
      logger.info("{} VAULT config records have been migrated into the common secret manager collection.", count);
    }
  }
}
