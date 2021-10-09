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

  @Inject
  public RestrictionHandlerFactory(
      @Named("availabilityRestrictionHandler") RestrictionHandler availabilityRestrictionHandler,
      @Named("staticLimitRestrictionHandler") RestrictionHandler staticLimitRestrictionHandler,
      @Named("rateLimitRestrictionHandler") RestrictionHandler rateLimitRestrictionHandler,
      @Named("customRestrictionHandler") RestrictionHandler customRestrictionHandler,
      @Named("durationRestrictionHandler") RestrictionHandler durationRestrictionHandler) {
    this.availabilityRestrictionHandler = availabilityRestrictionHandler;
    this.staticLimitRestrictionHandler = staticLimitRestrictionHandler;
    this.rateLimitRestrictionHandler = rateLimitRestrictionHandler;
    this.customRestrictionHandler = customRestrictionHandler;
    this.durationRestrictionHandler = durationRestrictionHandler;
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
      default:
        throw new IllegalArgumentException("Unknown restriction type");
    }
  }
}
