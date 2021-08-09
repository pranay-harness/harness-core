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
