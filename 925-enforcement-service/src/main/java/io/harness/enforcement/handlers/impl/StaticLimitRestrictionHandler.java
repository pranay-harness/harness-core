/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.handlers.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.bases.StaticLimitRestriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.details.StaticLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.LimitExceededException;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionUtils;
import io.harness.licensing.Edition;

public class StaticLimitRestrictionHandler implements RestrictionHandler {
  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, staticLimitRestriction, accountIdentifier);
    checkWithUsage(
        featureRestrictionName, staticLimitRestriction, accountIdentifier, currentCount, moduleType, edition);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount, ModuleType moduleType, Edition edition) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    if (!RestrictionUtils.isAvailable(
            currentCount, staticLimitRestriction.getLimit(), staticLimitRestriction.isAllowedIfEqual())) {
      throw new LimitExceededException(
          String.format("Exceeded static limitation. Current Limit: %s", staticLimitRestriction.getLimit()));
    }
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, Edition edition, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    long currentCount = getCurrentCount(featureRestrictionName, staticLimitRestriction, accountIdentifier);
    long limit = staticLimitRestriction.getLimit();

    featureDetailsDTO.setRestrictionType(staticLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(StaticLimitRestrictionDTO.builder().limit(limit).count(currentCount).build());
    featureDetailsDTO.setAllowed(
        RestrictionUtils.isAvailable(currentCount, limit, staticLimitRestriction.isAllowedIfEqual()));
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    return generateMetadataDTO(restriction);
  }

  private StaticLimitRestrictionMetadataDTO generateMetadataDTO(Restriction restriction) {
    StaticLimitRestriction staticLimitRestriction = (StaticLimitRestriction) restriction;
    return StaticLimitRestrictionMetadataDTO.builder()
        .restrictionType(staticLimitRestriction.getRestrictionType())
        .limit(staticLimitRestriction.getLimit())
        .allowedIfEqual(staticLimitRestriction.isAllowedIfEqual())
        .build();
  }

  private long getCurrentCount(FeatureRestrictionName featureRestrictionName,
      StaticLimitRestriction staticLimitRestriction, String accountIdentifier) {
    RestrictionMetadataDTO metadataDTO = generateMetadataDTO(staticLimitRestriction);
    return RestrictionUtils.getCurrentUsage(
        staticLimitRestriction, featureRestrictionName, accountIdentifier, metadataDTO);
  }
}
