/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.govern;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Provider;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@OwnedBy(HarnessTeam.PL)
public class ProviderMethodInterceptor implements MethodInterceptor {
  private Provider<? extends MethodInterceptor> provider;

  public <T> ProviderMethodInterceptor(Provider<T> provider) {
    this.provider = (Provider<? extends MethodInterceptor>) provider;
  }

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    return provider.get().invoke(methodInvocation);
  }
}
