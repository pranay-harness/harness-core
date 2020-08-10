package io.harness.ng.core;

import com.google.inject.AbstractModule;

import io.harness.ng.core.api.NGSecretFileService;
import io.harness.ng.core.api.NGSecretFileServiceImpl;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.NGSecretService;
import io.harness.ng.core.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.api.impl.NGSecretServiceImpl;

public class SecretManagementModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(NGSecretManagerService.class).to(NGSecretManagerServiceImpl.class);
    bind(NGSecretService.class).to(NGSecretServiceImpl.class);
    bind(NGSecretFileService.class).to(NGSecretFileServiceImpl.class);
  }
}
