/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.beans.SecretManagerConfig;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.LocalSecretManagerService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddRestrictionsToSecretManagerConfig implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManager secretManager;
  @Inject private LocalSecretManagerService localSecretManagerService;

  @Override
  public void migrate() {
    try (HIterator<SecretManagerConfig> iterator =
             new HIterator<>(wingsPersistence.createQuery(SecretManagerConfig.class).fetch())) {
      for (SecretManagerConfig secretManagerConfig : iterator) {
        secretManager.updateUsageRestrictionsForSecretManagerConfig(secretManagerConfig.getAccountId(),
            secretManagerConfig.getUuid(),
            localSecretManagerService.getEncryptionConfig(secretManagerConfig.getAccountId()).getUsageRestrictions());
      }
    }
  }
}
