/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.account;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
public abstract class AbstractAccountModule extends AbstractModule {
  @Override
  protected void configure() {
    install(AccountModule.getInstance());
  }

  @Provides
  @Singleton
  protected AccountConfig injectAccountConfiguration() {
    return accountConfiguration();
  }

  public abstract AccountConfig accountConfiguration();
}
