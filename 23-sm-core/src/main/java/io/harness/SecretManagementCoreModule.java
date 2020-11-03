package io.harness;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.persistence.HPersistence;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secrets.SecretsAuditService;
import io.harness.secrets.SecretsDao;
import io.harness.secrets.SecretsDaoImpl;
import io.harness.secrets.SecretsFileService;
import io.harness.secrets.SecretsRBACService;
import io.harness.secrets.setupusage.SecretSetupUsageService;
import io.harness.secrets.setupusage.SecretSetupUsageServiceImpl;
import io.harness.secrets.validation.BaseSecretValidator;
import io.harness.secrets.validation.SecretValidator;
import io.harness.secrets.validation.SecretValidators;
import io.harness.secrets.validation.validators.AwsSecretManagerValidator;
import io.harness.secrets.validation.validators.AzureSecretManagerValidator;
import io.harness.secrets.validation.validators.VaultSecretManagerValidator;

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

    binder()
        .bind(SecretValidator.class)
        .annotatedWith(Names.named(SecretValidators.AWS_SECRET_MANAGAER_VALIDATOR.getName()))
        .to(AwsSecretManagerValidator.class);

    binder()
        .bind(SecretValidator.class)
        .annotatedWith(Names.named(SecretValidators.AZURE_SECRET_MANAGER_VALIDATOR.getName()))
        .to(AzureSecretManagerValidator.class);

    binder()
        .bind(SecretValidator.class)
        .annotatedWith(Names.named(SecretValidators.VAULT_SECRET_MANAGER_VALIDATOR.getName()))
        .to(VaultSecretManagerValidator.class);

    binder()
        .bind(SecretValidator.class)
        .annotatedWith(Names.named(SecretValidators.COMMON_SECRET_MANAGER_VALIDATOR.getName()))
        .to(BaseSecretValidator.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
    requireBinding(SecretManagerConfigService.class);
    requireBinding(SecretsFileService.class);
    requireBinding(SecretsAuditService.class);
    requireBinding(SecretsRBACService.class);
  }
}