/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.logging.Misc;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

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
