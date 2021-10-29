package io.harness.licensing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
public abstract class AbstractLicenseModule extends AbstractModule {
  @Override
  protected void configure() {
    install(LicenseModule.getInstance());
  }

  @Provides
  @Singleton
  protected LicenseConfig injectAccountConfiguration() {
    return licenseConfiguration();
  }

  public abstract LicenseConfig licenseConfiguration();
}
