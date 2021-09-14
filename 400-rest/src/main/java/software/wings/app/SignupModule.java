/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.app;

import software.wings.service.intfc.SignupHandler;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.signup.MarketoSignupHandler;
import software.wings.signup.BlackListedDomainChecker;
import software.wings.signup.SignupServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SignupModule extends AbstractModule {
  @Override
  protected void configure() {
    this.bind(SignupService.class).to(SignupServiceImpl.class);
    this.bind(SignupHandler.class).to(MarketoSignupHandler.class);
  }

  @Provides
  @Singleton
  public BlackListedDomainChecker blackListedDomainChecker(MainConfiguration configuration) {
    return new BlackListedDomainChecker(configuration);
  }
}
