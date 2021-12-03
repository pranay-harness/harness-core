package io.harness.rule;

import software.wings.security.authentication.TotpChecker;
import software.wings.security.authentication.totp.FeatureFlaggedTotpChecker;
import software.wings.security.authentication.totp.SimpleTotpChecker;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class TestTotpModule extends AbstractModule {
  @Provides
  @Singleton
  @Named("featureFlagged")
  public TotpChecker<? super FeatureFlaggedTotpChecker.Request> checker() {
    return new SimpleTotpChecker<>();
  }
}
