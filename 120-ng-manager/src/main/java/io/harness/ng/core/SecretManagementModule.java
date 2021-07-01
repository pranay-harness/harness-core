package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.api.NGSecretActivityService;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.api.impl.NGEncryptedDataServiceImpl;
import io.harness.ng.core.api.impl.NGSecretActivityServiceImpl;
import io.harness.ng.core.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.api.impl.NGSecretServiceV2Impl;
import io.harness.ng.core.api.impl.NGSecretsFileServiceImpl;
import io.harness.ng.core.api.impl.SecretCrudServiceImpl;
import io.harness.ng.core.dao.NGEncryptedDataDao;
import io.harness.ng.core.dao.impl.NGEncryptedDaoServiceImpl;
import io.harness.secrets.SecretsFileService;

import software.wings.service.intfc.FileService;

import com.google.inject.AbstractModule;

@OwnedBy(PL)
public class SecretManagementModule extends AbstractModule {
  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(SecretsFileService.class).to(NGSecretsFileServiceImpl.class);
    bind(NGEncryptedDataDao.class).to(NGEncryptedDaoServiceImpl.class);
    bind(NGEncryptedDataService.class).to(NGEncryptedDataServiceImpl.class);
    bind(NGSecretManagerService.class).to(NGSecretManagerServiceImpl.class);
    bind(NGSecretServiceV2.class).to(NGSecretServiceV2Impl.class);
    bind(SecretCrudService.class).to(SecretCrudServiceImpl.class);
    bind(NGSecretActivityService.class).to(NGSecretActivityServiceImpl.class);
  }

  private void registerRequiredBindings() {
    requireBinding(FileService.class);
  }
}
