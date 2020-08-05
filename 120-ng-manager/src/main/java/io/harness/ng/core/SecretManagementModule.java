package io.harness.ng.core;

import com.google.inject.AbstractModule;

import io.harness.ng.core.services.api.NGSecretFileService;
import io.harness.ng.core.services.api.NGSecretFileServiceImpl;
import io.harness.ng.core.services.api.NGSecretManagerService;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.ng.core.services.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.services.api.impl.NGSecretServiceImpl;

public class SecretManagementModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(NGSecretManagerService.class).to(NGSecretManagerServiceImpl.class);
    bind(NGSecretService.class).to(NGSecretServiceImpl.class);
    bind(NGSecretFileService.class).to(NGSecretFileServiceImpl.class);
  }
}
