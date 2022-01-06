/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.handlers.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.LicenseRateLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.details.LicenseRateLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.LicenseRateLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.services.LicenseService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class LicenseRateLimitRestrictionHandler implements RestrictionHandler {
  private final LicenseService licenseService;

  @Inject
  public LicenseRateLimitRestrictionHandler(LicenseService licenseService) {
    this.licenseService = licenseService;
  }

  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition) {
    checkWithUsage(featureRestrictionName, restriction, accountIdentifier, 0, moduleType, edition);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount, ModuleType moduleType, Edition edition) {
    // always allowed at BE
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, Edition edition, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    LicenseRateLimitRestriction licenseRateLimitRestriction = (LicenseRateLimitRestriction) restriction;
    long limit = getLimit(licenseRateLimitRestriction, accountIdentifier, featureDetailsDTO.getModuleType());
    long currentCount = getCurrentCount(featureRestrictionName, licenseRateLimitRestriction, accountIdentifier, limit);

    featureDetailsDTO.setRestrictionType(licenseRateLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(LicenseRateLimitRestrictionDTO.builder()
                                         .limit(limit)
                                         .count(currentCount)
                                         .timeUnit(licenseRateLimitRestriction.getTimeUnit())
                                         .build());
    featureDetailsDTO.setAllowed(true);
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    return generateMetadataDTO(restriction, accountIdentifier, moduleType);
  }

  private LicenseRateLimitRestrictionMetadataDTO generateMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    LicenseRateLimitRestriction licenseRateLimitRestriction = (LicenseRateLimitRestriction) restriction;

    LicenseRateLimitRestrictionMetadataDTO result =
        LicenseRateLimitRestrictionMetadataDTO.builder()
            .restrictionType(licenseRateLimitRestriction.getRestrictionType())
            .fieldName(licenseRateLimitRestriction.getFieldName())
            .timeUnit(licenseRateLimitRestriction.getTimeUnit())
            .build();
    // In case of FE queries all metadata, skip the limit as it account dependent
    if (accountIdentifier != null && moduleType != null) {
      result.setLimit(getLimit(licenseRateLimitRestriction, accountIdentifier, moduleType));
    }

    return result;
  }

  private long getCurrentCount(FeatureRestrictionName featureRestrictionName, LicenseRateLimitRestriction restriction,
      String accountIdentifier, long limit) {
    RateLimitRestrictionMetadataDTO rateLimitRestrictionMetadataDTO = RateLimitRestrictionMetadataDTO.builder()
                                                                          .restrictionType(RestrictionType.RATE_LIMIT)
                                                                          .limit(limit)
                                                                          .timeUnit(restriction.getTimeUnit())
                                                                          .build();
    return RestrictionUtils.getCurrentUsage(
        restriction, featureRestrictionName, accountIdentifier, rateLimitRestrictionMetadataDTO);
  }

  private long getLimit(LicenseRateLimitRestriction restriction, String accountIdentifier, ModuleType moduleType) {
    LicensesWithSummaryDTO licenseSummary = licenseService.getLicenseSummary(accountIdentifier, moduleType);
    String fieldName = restriction.getFieldName();
    Field field = null;
    try {
      field = licenseSummary.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      if (Long.TYPE.equals(field.getType())) {
        return field.getLong(licenseSummary);
      } else if (Integer.TYPE.equals(field.getType())) {
        return field.getInt(licenseSummary);
      } else {
        log.error("Unsupported type [{}] for field [{}] in license summary", field.getType(), field.getName());
        return 0;
      }
    } catch (NoSuchFieldException e) {
      log.error("Field {} can't be found in {} license summary", fieldName, moduleType.name());
      throw new InvalidArgumentsException("Unable to get limitation from license", e, WingsException.USER_SRE);
    } catch (IllegalAccessException e) {
      log.error("Unable to access Field {} in {} license summary", fieldName, moduleType.name());
      throw new InvalidArgumentsException("Unable to get limitation from license", e, WingsException.USER_SRE);
    } finally {
      if (field != null) {
        field.setAccessible(false);
      }
    }
  }
}
