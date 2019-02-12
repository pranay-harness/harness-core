package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.security.encryption.EncryptionType;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.KmsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Previously all Vault secret manager's root token is encrypted using the KMS when KMS is present at time of creation.
 * This is resulting in a mutual dependency. We are breaking such dependency HAR-9053, so that the KMS encrypted Vault
 * root token need to be migrated into LOCAL encryption.
 *
 * @author marklu on 2019-02-11
 */
public class VaultTokenMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(VaultTokenMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private KmsService kmsService;

  @Override
  public void migrate() {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                     .field("type")
                                     .equal(SettingVariableTypes.VAULT)
                                     .field("encryptionType")
                                     .equal(EncryptionType.KMS);

    int migrationCount = 0;
    try (HIterator<EncryptedData> encryptedTokenRecords = new HIterator<>(query.fetch())) {
      while (encryptedTokenRecords.hasNext()) {
        EncryptedData encryptedTokenRecord = encryptedTokenRecords.next();
        String accountId = encryptedTokenRecord.getAccountId();
        String uuid = encryptedTokenRecord.getUuid();
        try {
          char[] decryptedToken = kmsService.decrypt(
              encryptedTokenRecord, accountId, kmsService.getKmsConfig(accountId, encryptedTokenRecord.getKmsId()));

          final String encryptionKey = UUID.randomUUID().toString();
          final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptionKey);
          char[] encryptedToken = simpleEncryption.encryptChars(decryptedToken);

          // Update the same encrypted record with LOCAL encryption type/key/value.
          Map<String, Object> map = new HashMap<>();
          map.put("encryptionKey", encryptionKey);
          map.put("encryptedValue", encryptedToken);
          map.put("encryptionType", EncryptionType.LOCAL);
          map.put("kmsId", "");
          wingsPersistence.updateFields(EncryptedData.class, uuid, map);

          migrationCount++;
        } catch (Exception e) {
          // Log the exception and carry on the migration.
          logger.error(
              "Failed to migrate Vault token record '" + encryptedTokenRecord.getName() + "' with id '" + uuid + "'.",
              e);
        }
      }
    } catch (Exception e) {
      logger.error("Failed to load all Vault encrypted token records for migration.", e);
    }

    logger.info("Successfully migrated {} Vault root tokens out of KMS.", migrationCount);
  }
}
