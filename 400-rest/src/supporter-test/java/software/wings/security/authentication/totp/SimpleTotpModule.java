package software.wings.security.authentication.totp;

import software.wings.security.authentication.TotpChecker;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class SimpleTotpModule extends AbstractModule {
  @Provides
  @Singleton
  @Named("featureFlagged")
  public TotpChecker<? super FeatureFlaggedTotpChecker.Request> checker() {
    return new SimpleTotpChecker<>();
  }
}
