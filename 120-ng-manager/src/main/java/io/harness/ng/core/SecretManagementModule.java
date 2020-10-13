package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.ng.core.api.NGSecretFileService;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.NGSecretService;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.api.impl.NGSecretFileServiceImpl;
import io.harness.ng.core.api.impl.NGSecretManagerServiceImpl;
import io.harness.ng.core.api.impl.NGSecretServiceImpl;
import io.harness.ng.core.api.impl.NGSecretServiceV2Impl;
import io.harness.ng.core.api.impl.SSHSecretServiceImpl;
import io.harness.ng.core.api.impl.SecretCrudServiceImpl;
import io.harness.ng.core.api.impl.SecretFileServiceImpl;
import io.harness.ng.core.api.impl.SecretTextServiceImpl;

public class SecretManagementModule extends AbstractModule {
  public static final String SSH_SECRET_SERVICE = "sshSecretService";
  public static final String SECRET_TEXT_SERVICE = "secretTextService";
  public static final String SECRET_FILE_SERVICE = "secretFileService";

  @Override
  protected void configure() {
    bind(NGSecretManagerService.class).to(NGSecretManagerServiceImpl.class);
    bind(NGSecretService.class).to(NGSecretServiceImpl.class);
    bind(NGSecretServiceV2.class).to(NGSecretServiceV2Impl.class);
    bind(SecretModifyService.class).annotatedWith(Names.named(SSH_SECRET_SERVICE)).to(SSHSecretServiceImpl.class);
    bind(SecretModifyService.class).annotatedWith(Names.named(SECRET_TEXT_SERVICE)).to(SecretTextServiceImpl.class);
    bind(SecretModifyService.class).annotatedWith(Names.named(SECRET_FILE_SERVICE)).to(SecretFileServiceImpl.class);
    bind(SecretCrudService.class).to(SecretCrudServiceImpl.class);
    bind(NGSecretFileService.class).to(NGSecretFileServiceImpl.class);
  }
}
