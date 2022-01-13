/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.handlers.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.RateLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.details.RateLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.LimitExceededException;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionUtils;
import io.harness.licensing.Edition;

import com.google.inject.Singleton;

@Singleton
public class RateLimitRestrictionHandler implements RestrictionHandler {
  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, rateLimitRestriction, accountIdentifier);
    checkWithUsage(featureRestrictionName, rateLimitRestriction, accountIdentifier, currentCount, moduleType, edition);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount, ModuleType moduleType, Edition edition) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    if (!RestrictionUtils.isAvailable(
            currentCount, rateLimitRestriction.getLimit(), rateLimitRestriction.isAllowedIfEqual())) {
      throw new LimitExceededException(
          String.format("Exceeded rate limitation. Current Limit: %s", rateLimitRestriction.getLimit()));
    }
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, Edition edition, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, rateLimitRestriction, accountIdentifier);
    long limit = rateLimitRestriction.getLimit();

    featureDetailsDTO.setRestrictionType(rateLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(RateLimitRestrictionDTO.builder()
                                         .limit(limit)
                                         .count(currentCount)
                                         .timeUnit(rateLimitRestriction.getTimeUnit())
                                         .build());
    featureDetailsDTO.setAllowed(
        RestrictionUtils.isAvailable(currentCount, limit, rateLimitRestriction.isAllowedIfEqual()));
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    return generateMetadataDTO(restriction);
  }

  private RateLimitRestrictionMetadataDTO generateMetadataDTO(Restriction restriction) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    return RateLimitRestrictionMetadataDTO.builder()
        .restrictionType(rateLimitRestriction.getRestrictionType())
        .limit(rateLimitRestriction.getLimit())
        .timeUnit(rateLimitRestriction.getTimeUnit())
        .allowedIfEqual(rateLimitRestriction.isAllowedIfEqual())
        .build();
  }

  private long getCurrentCount(FeatureRestrictionName featureRestrictionName, RateLimitRestriction rateLimitRestriction,
      String accountIdentifier) {
    RestrictionMetadataDTO metadataDTO = generateMetadataDTO(rateLimitRestriction);
    return RestrictionUtils.getCurrentUsage(
        rateLimitRestriction, featureRestrictionName, accountIdentifier, metadataDTO);
  }
}
