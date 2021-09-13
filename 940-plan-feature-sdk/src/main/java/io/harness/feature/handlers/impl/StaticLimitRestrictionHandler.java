package io.harness.feature.handlers.impl;

import io.harness.feature.bases.StaticLimitRestriction;
import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandler;

public class StaticLimitRestrictionHandler implements RestrictionHandler {
  private StaticLimitRestriction staticLimitRestriction;

  public StaticLimitRestrictionHandler(StaticLimitRestriction staticLimitRestriction) {
    this.staticLimitRestriction = staticLimitRestriction;
  }

  @Override
  public RestrictionType getRestrictionType() {
    return null;
  }

  @Override
  public void check(String accountIdentifier) {
    if (staticLimitRestriction.getStaticLimitInterface().getCurrentValue(accountIdentifier)
        >= staticLimitRestriction.getLimit()) {
      throw new LimitExceededException(
          String.format("Exceeded static limitation. Current Limit: %s", staticLimitRestriction.getLimit()));
    }
  }

  @Override
  public RestrictionDTO toRestrictionDTO(String accountIdentifier) {
    return RestrictionDTO.builder()
        .restrictionType(staticLimitRestriction.getRestrictionType())
        .limit(staticLimitRestriction.getLimit())
        .count(staticLimitRestriction.getStaticLimitInterface().getCurrentValue(accountIdentifier))
        .build();
  }
}
