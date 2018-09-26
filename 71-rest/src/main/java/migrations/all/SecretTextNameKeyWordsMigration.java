package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Misc;

public class SecretTextNameKeyWordsMigration implements Migration {
  private static Logger logger = LoggerFactory.getLogger(SecretTextNameKeyWordsMigration.class);
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<EncryptedData> iterator = new HIterator<>(wingsPersistence.createQuery(EncryptedData.class)
                                                                 .filter("type", SettingVariableTypes.SECRET_TEXT)
                                                                 .fetch())) {
      while (iterator.hasNext()) {
        EncryptedData encryptedData = iterator.next();
        logger.info("updating {} id {}", encryptedData.getName(), encryptedData.getUuid());
        encryptedData.addSearchTag(Misc.replaceDotWithUnicode(encryptedData.getName()));
        logger.info("updating search tags for {} to {}", encryptedData.getName(), encryptedData.getSearchTags());
        wingsPersistence.save(encryptedData);
      }
    }
  }
}