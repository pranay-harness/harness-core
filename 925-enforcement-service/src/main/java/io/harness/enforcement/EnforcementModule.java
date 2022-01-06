/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement;

import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionHandlerFactory;
import io.harness.enforcement.handlers.impl.AvailabilityRestrictionHandler;
import io.harness.enforcement.handlers.impl.CustomRestrictionHandler;
import io.harness.enforcement.handlers.impl.DurationRestrictionHandler;
import io.harness.enforcement.handlers.impl.LicenseRateLimitRestrictionHandler;
import io.harness.enforcement.handlers.impl.LicenseStaticLimitRestrictionHandler;
import io.harness.enforcement.handlers.impl.RateLimitRestrictionHandler;
import io.harness.enforcement.handlers.impl.StaticLimitRestrictionHandler;
import io.harness.enforcement.services.EnforcementService;
import io.harness.enforcement.services.FeatureRestrictionLoader;
import io.harness.enforcement.services.impl.EnforcementServiceImpl;
import io.harness.enforcement.services.impl.FeatureRestrictionLoaderImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class EnforcementModule extends AbstractModule {
  private static EnforcementModule instance;

  private EnforcementModule() {}

  public static EnforcementModule getInstance() {
    if (instance == null) {
      instance = new EnforcementModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(EnforcementService.class).to(EnforcementServiceImpl.class);
    bind(FeatureRestrictionLoader.class).to(FeatureRestrictionLoaderImpl.class);
    bind(RestrictionHandlerFactory.class);

    bind(RestrictionHandler.class)
        .annotatedWith(Names.named("availabilityRestrictionHandler"))
        .to(AvailabilityRestrictionHandler.class);
    bind(RestrictionHandler.class)
        .annotatedWith(Names.named("staticLimitRestrictionHandler"))
        .to(StaticLimitRestrictionHandler.class);
    bind(RestrictionHandler.class)
        .annotatedWith(Names.named("rateLimitRestrictionHandler"))
        .to(RateLimitRestrictionHandler.class);
    bind(RestrictionHandler.class)
        .annotatedWith(Names.named("customRestrictionHandler"))
        .to(CustomRestrictionHandler.class);
    bind(RestrictionHandler.class)
        .annotatedWith(Names.named("durationRestrictionHandler"))
        .to(DurationRestrictionHandler.class);
    bind(RestrictionHandler.class)
        .annotatedWith(Names.named("licenseRateLimitRestrictionHandler"))
        .to(LicenseRateLimitRestrictionHandler.class);
    bind(RestrictionHandler.class)
        .annotatedWith(Names.named("licenseStaticLimitRestrictionHandler"))
        .to(LicenseStaticLimitRestrictionHandler.class);
  }
}
