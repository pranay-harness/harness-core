/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
