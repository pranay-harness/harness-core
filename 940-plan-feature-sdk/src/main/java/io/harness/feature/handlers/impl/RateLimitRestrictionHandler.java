package io.harness.feature.handlers.impl;

import io.harness.feature.bases.RateLimitRestriction;
import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandler;

import java.time.Instant;

public class RateLimitRestrictionHandler implements RestrictionHandler {
  private final RateLimitRestriction rateLimitRestriction;

  public RateLimitRestrictionHandler(RateLimitRestriction rateLimitRestriction) {
    this.rateLimitRestriction = rateLimitRestriction;
  }

  @Override
  public RestrictionType getRestrictionType() {
    return rateLimitRestriction.getRestrictionType();
  }

  @Override
  public void check(String accountIdentifier) {
    if (getCurrentCount(accountIdentifier) >= rateLimitRestriction.getLimit()) {
      throw new LimitExceededException(
          String.format("Exceeded rate limitation. Current Limit: %s", rateLimitRestriction.getLimit()));
    }
  }

  @Override
  public RestrictionDTO toRestrictionDTO(String accountIdentifier) {
    return RestrictionDTO.builder()
        .restrictionType(rateLimitRestriction.getRestrictionType())
        .limit(rateLimitRestriction.getLimit())
        .count(getCurrentCount(accountIdentifier))
        .build();
  }

  private long getCurrentCount(String accountIdentifier) {
    Instant current = Instant.now();

    long startTime =
        current
            .minus(rateLimitRestriction.getTimeUnit().getNumberOfUnits(), rateLimitRestriction.getTimeUnit().getUnit())
            .toEpochMilli();
    long endTime = current.toEpochMilli();
    return rateLimitRestriction.getRateLimitInterface().getCurrentValue(accountIdentifier, startTime, endTime);
  }
}
