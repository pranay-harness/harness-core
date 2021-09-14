package io.harness.feature.handlers.impl;

import io.harness.feature.bases.StaticLimitRestriction;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.StaticLimitRestrictionDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandler;
import io.harness.feature.handlers.RestrictionUtils;

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
  public void check(FeatureRestriction featureName, String accountIdentifier) {
    long currentCount = getCurrentCount(featureName, accountIdentifier);
    checkWithUsage(featureName, accountIdentifier, currentCount);
  }

  @Override
  public void checkWithUsage(FeatureRestriction featureName, String accountIdentifier, long currentCount) {
    if (!RestrictionUtils.isAvailable(currentCount, staticLimitRestriction.getLimit())) {
      throw new LimitExceededException(
          String.format("Exceeded static limitation. Current Limit: %s", staticLimitRestriction.getLimit()));
    }
  }

  @Override
  public void fillRestrictionDTO(
      FeatureRestriction featureName, String accountIdentifier, FeatureDetailsDTO featureDetailsDTO) {
    long currentCount = getCurrentCount(featureName, accountIdentifier);
    long limit = staticLimitRestriction.getLimit();

    featureDetailsDTO.setRestrictionType(staticLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(StaticLimitRestrictionDTO.builder().limit(limit).count(currentCount).build());
    featureDetailsDTO.setAllowed(RestrictionUtils.isAvailable(currentCount, limit));
  }

  private long getCurrentCount(FeatureRestriction featureName, String accountIdentifier) {
    return RestrictionUtils.getCurrentUsage(staticLimitRestriction, featureName, accountIdentifier);
  }
}
