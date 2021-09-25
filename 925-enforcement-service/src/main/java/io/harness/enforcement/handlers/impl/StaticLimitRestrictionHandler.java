package io.harness.enforcement.handlers.impl;

import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.bases.StaticLimitRestriction;
import io.harness.enforcement.beans.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.StaticLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.LimitExceededException;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionUtils;

public class StaticLimitRestrictionHandler implements RestrictionHandler {
  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, staticLimitRestriction, accountIdentifier);
    checkWithUsage(featureRestrictionName, staticLimitRestriction, accountIdentifier, currentCount);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    if (!RestrictionUtils.isAvailable(currentCount, staticLimitRestriction.getLimit())) {
      throw new LimitExceededException(
          String.format("Exceeded static limitation. Current Limit: %s", staticLimitRestriction.getLimit()));
    }
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, staticLimitRestriction, accountIdentifier);
    long limit = staticLimitRestriction.getLimit();

    featureDetailsDTO.setRestrictionType(staticLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(StaticLimitRestrictionDTO.builder().limit(limit).count(currentCount).build());
    featureDetailsDTO.setAllowed(RestrictionUtils.isAvailable(currentCount, limit));
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(Restriction restriction) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    return StaticLimitRestrictionMetadataDTO.builder()
        .restrictionType(staticLimitRestriction.getRestrictionType())
        .limit(staticLimitRestriction.getLimit())
        .build();
  }

  private long getCurrentCount(FeatureRestrictionName featureRestrictionName,
      StaticLimitRestriction staticLimitRestriction, String accountIdentifier) {
    return RestrictionUtils.getCurrentUsage(staticLimitRestriction, featureRestrictionName, accountIdentifier);
  }
}
