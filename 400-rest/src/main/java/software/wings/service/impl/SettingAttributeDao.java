/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.mongodb.morphia.query.Query;

@Singleton
@OwnedBy(HarnessTeam.CDC)
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
