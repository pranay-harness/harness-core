package io.harness.feature.handlers.impl;

import io.harness.feature.bases.RateLimitRestriction;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.RateLimitRestrictionDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandler;
import io.harness.feature.handlers.RestrictionUtils;

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
  public void check(FeatureRestriction featureName, String accountIdentifier) {
    long currentCount = getCurrentCount(featureName, accountIdentifier);
    checkWithUsage(featureName, accountIdentifier, currentCount);
  }

  @Override
  public void checkWithUsage(FeatureRestriction featureName, String accountIdentifier, long currentCount) {
    if (!RestrictionUtils.isAvailable(currentCount, rateLimitRestriction.getLimit())) {
      throw new LimitExceededException(
          String.format("Exceeded rate limitation. Current Limit: %s", rateLimitRestriction.getLimit()));
    }
  }

  @Override
  public void fillRestrictionDTO(
      FeatureRestriction featureName, String accountIdentifier, FeatureDetailsDTO featureDetailsDTO) {
    long currentCount = getCurrentCount(featureName, accountIdentifier);
    long limit = rateLimitRestriction.getLimit();

    featureDetailsDTO.setRestrictionType(rateLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(RateLimitRestrictionDTO.builder().limit(limit).count(currentCount).build());
    featureDetailsDTO.setAllowed(RestrictionUtils.isAvailable(currentCount, limit));
  }

  private long getCurrentCount(FeatureRestriction featureName, String accountIdentifier) {
    return RestrictionUtils.getCurrentUsage(rateLimitRestriction, featureName, accountIdentifier);
  }
}
