package migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeValidate;

import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class AwsConfigEc2IamRoleMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<SettingAttribute> settingAttributes = wingsPersistence.createQuery(SettingAttribute.class, excludeValidate)
                                                   .filter(SettingAttribute.VALUE_TYPE_KEY, "AWS")
                                                   .filter("value.useEc2IamCredentials", true)
                                                   .field("value.encryptedSecretKey")
                                                   .exists()
                                                   .asList();
    log.info("SettingAttribute found {}", settingAttributes.size());
    settingAttributes.forEach(settingAttribute -> {
      try {
        log.info("Updating settingAttribute: {}", settingAttribute.getUuid());
        UpdateOperations<SettingAttribute> operations = wingsPersistence.createUpdateOperations(SettingAttribute.class);
        setUnset(operations, "value.encryptedSecretKey", null);
        wingsPersistence.update(settingAttribute, operations);
        log.info("Updated settingAttribute: {}", settingAttribute.getUuid());
      } catch (Exception ex) {
        log.error("Exception while updating setting attribute: " + settingAttribute.getUuid(), ex);
      }
    });
    log.info("Completed migration for Aws Config Ec2 Iam Role");
  }
}
