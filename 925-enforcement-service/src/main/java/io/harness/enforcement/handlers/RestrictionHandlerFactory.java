/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.handlers;

import io.harness.enforcement.constants.RestrictionType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class RestrictionHandlerFactory {
  private final RestrictionHandler availabilityRestrictionHandler;
  private final RestrictionHandler staticLimitRestrictionHandler;
  private final RestrictionHandler rateLimitRestrictionHandler;
  private final RestrictionHandler customRestrictionHandler;
  private final RestrictionHandler durationRestrictionHandler;
  private final RestrictionHandler licenseRateLimitRestrictionHandler;
  private final RestrictionHandler licenseStaticLimitRestrictionHandler;

  @Inject
  public RestrictionHandlerFactory(
      @Named("availabilityRestrictionHandler") RestrictionHandler availabilityRestrictionHandler,
      @Named("staticLimitRestrictionHandler") RestrictionHandler staticLimitRestrictionHandler,
      @Named("rateLimitRestrictionHandler") RestrictionHandler rateLimitRestrictionHandler,
      @Named("customRestrictionHandler") RestrictionHandler customRestrictionHandler,
      @Named("durationRestrictionHandler") RestrictionHandler durationRestrictionHandler,
      @Named("licenseRateLimitRestrictionHandler") RestrictionHandler licenseRateLimitRestrictionHandler,
      @Named("licenseStaticLimitRestrictionHandler") RestrictionHandler licenseStaticLimitRestrictionHandler) {
    this.availabilityRestrictionHandler = availabilityRestrictionHandler;
    this.staticLimitRestrictionHandler = staticLimitRestrictionHandler;
    this.rateLimitRestrictionHandler = rateLimitRestrictionHandler;
    this.customRestrictionHandler = customRestrictionHandler;
    this.durationRestrictionHandler = durationRestrictionHandler;
    this.licenseRateLimitRestrictionHandler = licenseRateLimitRestrictionHandler;
    this.licenseStaticLimitRestrictionHandler = licenseStaticLimitRestrictionHandler;
  }

  public RestrictionHandler getHandler(RestrictionType restrictionType) {
    switch (restrictionType) {
      case AVAILABILITY:
        return availabilityRestrictionHandler;
      case STATIC_LIMIT:
        return staticLimitRestrictionHandler;
      case RATE_LIMIT:
        return rateLimitRestrictionHandler;
      case CUSTOM:
        return customRestrictionHandler;
      case DURATION:
        return durationRestrictionHandler;
      case LICENSE_RATE_LIMIT:
        return licenseRateLimitRestrictionHandler;
      case LICENSE_STATIC_LIMIT:
        return licenseStaticLimitRestrictionHandler;
      default:
        throw new IllegalArgumentException("Unknown restriction type");
    }
  }
}
