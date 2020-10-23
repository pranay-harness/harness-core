package io.harness;

import com.google.inject.AbstractModule;

import io.harness.persistence.HPersistence;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secrets.SecretsDao;
import io.harness.secrets.SecretsDaoImpl;

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
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
    requireBinding(SecretManagerConfigService.class);
  }
}