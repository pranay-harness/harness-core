/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.handlers;

import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.bases.Feature;
import io.harness.feature.bases.RateLimitRestriction;
import io.harness.feature.bases.Restriction;
import io.harness.feature.bases.StaticLimitRestriction;
import io.harness.feature.exceptions.FeatureNotSupportedException;
import io.harness.feature.handlers.impl.EnableDisableRestrictionHandler;
import io.harness.feature.handlers.impl.RateLimitRestrictionHandler;
import io.harness.feature.handlers.impl.StaticLimitRestrictionHandler;
import io.harness.licensing.Edition;

import com.google.inject.Singleton;

@Singleton
public class RestrictionHandlerFactory {
  public RestrictionHandler getHandler(Feature feature, Edition edition) {
    Restriction restriction = feature.getRestrictions().get(edition);
    if (restriction == null) {
      throw new FeatureNotSupportedException("Invalid feature definition");
    }

    switch (restriction.getRestrictionType()) {
      case ENABLED:
        return new EnableDisableRestrictionHandler((EnableDisableRestriction) restriction);
      case STATIC_LIMIT:
        return new StaticLimitRestrictionHandler((StaticLimitRestriction) restriction);
      case RATE_LIMIT:
        return new RateLimitRestrictionHandler((RateLimitRestriction) restriction);
      default:
        throw new IllegalArgumentException("Unknown restriction type");
    }
  }
}
