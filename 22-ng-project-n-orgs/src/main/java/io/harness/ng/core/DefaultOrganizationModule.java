package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

@OwnedBy(PL)
public class DefaultOrganizationModule extends AbstractModule {
  @Override
  protected void configure() {
    DefaultOrganizationInterceptor interceptor = new DefaultOrganizationInterceptor();
    requestInjection(interceptor);
    bind(DefaultOrganizationInterceptor.class).toInstance(interceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(DefaultOrganization.class), interceptor);
    bindInterceptor(Matchers.annotatedWith(DefaultOrganization.class), Matchers.any(), interceptor);
  }
}
