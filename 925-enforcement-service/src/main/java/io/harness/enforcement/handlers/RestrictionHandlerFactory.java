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

  @Inject
  public RestrictionHandlerFactory(
      @Named("availabilityRestrictionHandler") RestrictionHandler availabilityRestrictionHandler,
      @Named("staticLimitRestrictionHandler") RestrictionHandler staticLimitRestrictionHandler,
      @Named("rateLimitRestrictionHandler") RestrictionHandler rateLimitRestrictionHandler) {
    this.availabilityRestrictionHandler = availabilityRestrictionHandler;
    this.staticLimitRestrictionHandler = staticLimitRestrictionHandler;
    this.rateLimitRestrictionHandler = rateLimitRestrictionHandler;
  }
  public RestrictionHandler getHandler(RestrictionType restrictionType) {
    switch (restrictionType) {
      case AVAILABILITY:
        return availabilityRestrictionHandler;
      case STATIC_LIMIT:
        return staticLimitRestrictionHandler;
      case RATE_LIMIT:
        return rateLimitRestrictionHandler;
      default:
        throw new IllegalArgumentException("Unknown restriction type");
    }
  }
}
