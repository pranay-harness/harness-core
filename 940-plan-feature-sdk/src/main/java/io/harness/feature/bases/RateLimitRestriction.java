package io.harness.feature.bases;

import io.harness.feature.TimeUnit;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.RateLimitInterface;

import java.time.Instant;

public class RateLimitRestriction extends Restriction {
  long limit;
  TimeUnit timeUnit;
  RateLimitInterface rateLimit;

  public RateLimitRestriction(
      RestrictionType restrictionType, long limit, TimeUnit timeUnit, RateLimitInterface rateLimit) {
    super(restrictionType);
    this.limit = limit;
    this.timeUnit = timeUnit;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean check(String accountIdentifier) {
    Instant current = Instant.now();
    long startTime = current.minus(timeUnit.getNumberOfUnits(), timeUnit.getUnit()).toEpochMilli();
    long endTime = current.toEpochMilli();
    return rateLimit.getCurrentValue(accountIdentifier, startTime, endTime) < limit;
  }
}
