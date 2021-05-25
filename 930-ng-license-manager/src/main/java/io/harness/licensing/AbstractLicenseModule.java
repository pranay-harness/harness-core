package io.harness.licensing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public abstract class AbstractLicenseModule extends AbstractModule {
  @Override
  protected void configure() {
    install(LicenseModule.getInstance());
  }

  @Provides
  @Singleton
  protected LicenseConfiguration injectLicenseConfiguration() {
    return licenseConfiguration();
  }

  public abstract LicenseConfiguration licenseConfiguration();
}
