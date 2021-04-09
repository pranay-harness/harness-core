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