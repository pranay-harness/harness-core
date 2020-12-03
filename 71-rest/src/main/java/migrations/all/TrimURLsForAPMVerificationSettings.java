package migrations.all;

import static io.harness.persistence.HQuery.excludeValidate;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.lang3.reflect.FieldUtils;

@Slf4j
public class TrimURLsForAPMVerificationSettings implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<SettingAttribute> settingAttributes =
        wingsPersistence.createQuery(SettingAttribute.class, excludeValidate)
            .filter(SettingAttributeKeys.value_type + " in",
                Arrays.asList("BUG_SNAG", "APM_VERIFICATION", "PROMETHEUS", "DATA_DOG", "DATA_DOG_LOG"))
            .filter("value.url", Pattern.compile("^\\s+|\\s+$"))
            .asList();
    log.info("settingAttribute found {}", settingAttributes.size());
    settingAttributes.forEach(settingAttribute -> {
      try {
        log.info("Updating settingAttribute: {}", settingAttribute.getUuid());
        String url = (String) FieldUtils.readField(
            settingAttribute.getValue().getClass().getDeclaredField("url"), settingAttribute.getValue(), true);
        log.info("URL is {}", url);
        wingsPersistence.updateField(SettingAttribute.class, settingAttribute.getUuid(), "value.url", url.trim());
        log.info("Updated settingAttribute: {} url to {}", settingAttribute.getUuid(), url.trim());
      } catch (IllegalAccessException | NoSuchFieldException e) {
        throw new IllegalStateException(e);
      }
    });
  }
}
