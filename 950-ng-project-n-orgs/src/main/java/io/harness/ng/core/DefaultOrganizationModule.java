/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
