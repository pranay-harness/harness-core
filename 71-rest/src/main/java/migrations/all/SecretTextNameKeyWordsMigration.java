package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Misc;
@Slf4j
public class SecretTextNameKeyWordsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<EncryptedData> iterator =
             new HIterator<>(wingsPersistence.createQuery(EncryptedData.class)
                                 .filter(EncryptedDataKeys.type, SettingVariableTypes.SECRET_TEXT)
                                 .fetch())) {
      for (EncryptedData encryptedData : iterator) {
        logger.info("updating {} id {}", encryptedData.getName(), encryptedData.getUuid());
        encryptedData.addSearchTag(Misc.replaceDotWithUnicode(encryptedData.getName()));
        logger.info("updating search tags for {} to {}", encryptedData.getName(), encryptedData.getSearchTags());
        wingsPersistence.save(encryptedData);
      }
    }
  }
}