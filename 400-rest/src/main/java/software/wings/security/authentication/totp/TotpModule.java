/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.totp;

import io.harness.ff.FeatureFlagService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.persistence.HPersistence;
import io.harness.sanitizer.HtmlInputSanitizer;

import software.wings.app.MainConfiguration;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.security.authentication.TotpChecker;
import software.wings.service.intfc.EmailNotificationService;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Clock;

public class TotpModule extends AbstractModule {
  @Provides
  @Singleton
  @Named("featureFlagged")
  public TotpChecker<? super FeatureFlaggedTotpChecker.Request> checker(Clock clock, HPersistence persistence,
      MainConfiguration mainConfiguration, SubdomainUrlHelperIntfc subdomainUrlHelper,
      HtmlInputSanitizer htmlInputSanitizer, EmailNotificationService emailNotificationService,
      FeatureFlagService featureFlagService) {
    RateLimitProtectionRepository repository = new RateLimitProtectionMongoRepository(persistence);

    TotpConfig totpConfig = mainConfiguration.getTotpConfig();
    RateLimit rateLimit = totpConfig.getLimit().getRateLimit();

    NotificationService notificationService = new NotificationServiceImpl(
        persistence, subdomainUrlHelper, emailNotificationService, htmlInputSanitizer, mainConfiguration);

    SimpleTotpChecker<SimpleTotpChecker.Request> simpleChecker = new SimpleTotpChecker<>();

    RateLimitedTotpChecker<RateLimitedTotpChecker.Request> rateLimitedChecker =
        new RateLimitedTotpChecker<>(simpleChecker, repository, rateLimit, notificationService, clock,
            totpConfig.getIncorrectAttemptsUntilSecOpsNotified());

    return new FeatureFlaggedTotpChecker<>(rateLimitedChecker, simpleChecker, featureFlagService);
  }
}
