package io.harness.enforcement.handlers.impl;

import io.harness.enforcement.bases.RateLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.RateLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.LimitExceededException;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionUtils;

import com.google.inject.Singleton;

@Singleton
public class RateLimitRestrictionHandler implements RestrictionHandler {
  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, rateLimitRestriction, accountIdentifier);
    checkWithUsage(featureRestrictionName, rateLimitRestriction, accountIdentifier, currentCount);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    if (!RestrictionUtils.isAvailable(currentCount, rateLimitRestriction.getLimit())) {
      throw new LimitExceededException(
          String.format("Exceeded rate limitation. Current Limit: %s", rateLimitRestriction.getLimit()));
    }
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, rateLimitRestriction, accountIdentifier);
    long limit = rateLimitRestriction.getLimit();

    featureDetailsDTO.setRestrictionType(rateLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(RateLimitRestrictionDTO.builder().limit(limit).count(currentCount).build());
    featureDetailsDTO.setAllowed(RestrictionUtils.isAvailable(currentCount, limit));
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(Restriction restriction) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    return RateLimitRestrictionMetadataDTO.builder()
        .restrictionType(rateLimitRestriction.getRestrictionType())
        .limit(rateLimitRestriction.getLimit())
        .timeUnit(rateLimitRestriction.getTimeUnit())
        .build();
  }

  private long getCurrentCount(FeatureRestrictionName featureRestrictionName, RateLimitRestriction rateLimitRestriction,
      String accountIdentifier) {
    return RestrictionUtils.getCurrentUsage(rateLimitRestriction, featureRestrictionName, accountIdentifier);
  }
}
