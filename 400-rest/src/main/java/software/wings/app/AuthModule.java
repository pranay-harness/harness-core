/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.app;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.AuthServiceImpl;
import software.wings.service.intfc.AuthService;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class AuthModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(AuthService.class).to(AuthServiceImpl.class);
  }
}
