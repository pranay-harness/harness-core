package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;

import java.util.List;

@Singleton
public class SettingAttributeDao {
  @Inject private HPersistence persistence;

  public String save(SettingAttribute settingAttribute) {
    return persistence.save(settingAttribute);
  }

  public List<SettingAttribute> list(String accountId, SettingCategory category) {
    Query<SettingAttribute> query = persistence.createQuery(SettingAttribute.class)
                                        .field(SettingAttributeKeys.accountId)
                                        .equal(accountId)
                                        .field(SettingAttributeKeys.category)
                                        .equal(category);
    return query.asList();
  }
}
