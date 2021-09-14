/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderMethodInterceptor;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

@OwnedBy(PL)
public class DefaultOrganizationModule extends AbstractModule {
  @Override
  protected void configure() {
    ProviderMethodInterceptor interceptor =
        new ProviderMethodInterceptor(getProvider(DefaultOrganizationInterceptor.class));
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(DefaultOrganization.class), interceptor);
    bindInterceptor(Matchers.annotatedWith(DefaultOrganization.class), Matchers.any(), interceptor);
  }
}
