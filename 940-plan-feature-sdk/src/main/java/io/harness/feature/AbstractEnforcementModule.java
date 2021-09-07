package io.harness.feature;

import io.harness.licensing.remote.NgLicenseHttpClientModule;
import io.harness.remote.client.ServiceHttpClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public abstract class AbstractEnforcementModule extends AbstractModule {
  @Override
  protected void configure() {
    install(EnforcementModule.getInstance());
    install(NgLicenseHttpClientModule.getInstance(ngManagerClientConfig(), serviceSecret(), clientId()));
  }

  @Provides
  @Singleton
  protected EnforcementConfiguration injectEnforcementConfiguration() {
    return enforcementConfiguration();
  }

  public abstract EnforcementConfiguration enforcementConfiguration();
  public abstract ServiceHttpClientConfig ngManagerClientConfig();
  public abstract String serviceSecret();
  public abstract String clientId();
}
