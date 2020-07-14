package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.dl.WingsPersistence;
import software.wings.settings.SettingVariableTypes;

/**
 * @author marklu on 9/4/19
 */
@Slf4j
public class SettingAttributesCategoryMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    Query<SettingAttribute> settingAttributeWithoutCategoryQuery =
        wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority)
            .field(SettingAttributeKeys.category)
            .doesNotExist();

    int count = 0;
    int updateCount = 0;
    try (
        HIterator<SettingAttribute> settingAttributes = new HIterator<>(settingAttributeWithoutCategoryQuery.fetch())) {
      while (settingAttributes.hasNext()) {
        SettingAttribute settingAttribute = settingAttributes.next();
        SettingCategory category =
            SettingCategory.getCategory(SettingVariableTypes.valueOf(settingAttribute.getValue().getType()));
        if (category != null) {
          UpdateOperations<SettingAttribute> updateOperations =
              wingsPersistence.createUpdateOperations(SettingAttribute.class)
                  .set(SettingAttributeKeys.category, category.name());
          wingsPersistence.update(settingAttribute, updateOperations);

          logger.info("Setting attribute '{}' with id {} has been updated to category {}", settingAttribute.getName(),
              settingAttribute.getUuid(), category);
          updateCount++;
        }
        count++;
      }
    }

    logger.info("Found {} setting attributes with category from database. Updated {} of them with proper categories",
        count, updateCount);
  }
}
