package migrations.all;

import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.logging.Misc;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
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
        log.info("updating {} id {}", encryptedData.getName(), encryptedData.getUuid());
        encryptedData.addSearchTag(Misc.replaceDotWithUnicode(encryptedData.getName()));
        log.info("updating search tags for {} to {}", encryptedData.getName(), encryptedData.getSearchTags());
        wingsPersistence.save(encryptedData);
      }
    }
  }
}
