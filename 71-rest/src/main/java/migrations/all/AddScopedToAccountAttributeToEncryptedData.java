package migrations.all;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.settings.SettingValue.SettingVariableTypes;

@Slf4j
public class AddScopedToAccountAttributeToEncryptedData implements Migration {
  private WingsPersistence wingsPersistence;

  @Inject
  public AddScopedToAccountAttributeToEncryptedData(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    logger.info("Executing migration AddScopedToAccountAttributeToEncryptedData");
    UpdateOperations<EncryptedData> updateOperations = wingsPersistence.createUpdateOperations(EncryptedData.class)
                                                           .set(EncryptedDataKeys.scopedToAccount, Boolean.TRUE);

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class)
            .field(EncryptedDataKeys.accountId)
            .exists()
            .field(EncryptedDataKeys.type)
            .hasAnyOf(Sets.newHashSet(SettingVariableTypes.SECRET_TEXT.name(), SettingVariableTypes.CONFIG_FILE.name()))
            .field(EncryptedDataKeys.usageRestrictions)
            .doesNotExist();

    UpdateResults result = wingsPersistence.update(query, updateOperations);

    logger.info("Migration AddScopedToAccountAttributeToEncryptedData updated {} EncryptedData records",
        result.getUpdatedCount());
  }
}
