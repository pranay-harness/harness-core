package io.harness;

import com.google.inject.AbstractModule;

import io.harness.persistence.HPersistence;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secrets.SecretsDao;
import io.harness.secrets.SecretsDaoImpl;
import io.harness.secrets.setupusage.SecretSetupUsageService;
import io.harness.secrets.setupusage.SecretSetupUsageServiceImpl;

public class SecretManagementCoreModule extends AbstractModule {
  private static SecretManagementCoreModule instance;

  public static SecretManagementCoreModule getInstance() {
    if (instance == null) {
      instance = new SecretManagementCoreModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(SecretsDao.class).to(SecretsDaoImpl.class);
    bind(SecretSetupUsageService.class).to(SecretSetupUsageServiceImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
    requireBinding(SecretManagerConfigService.class);
  }
}