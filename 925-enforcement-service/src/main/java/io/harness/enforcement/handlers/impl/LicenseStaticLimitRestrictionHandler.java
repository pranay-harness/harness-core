/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.handlers.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.LicenseStaticLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.details.LicenseStaticLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.LicenseStaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionUtils;
import io.harness.enforcement.interfaces.LicenseLimitInterface;
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
public class LicenseStaticLimitRestrictionHandler implements RestrictionHandler {
  private final LicenseService licenseService;

  @Inject
  public LicenseStaticLimitRestrictionHandler(LicenseService licenseService) {
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
    LicenseStaticLimitRestriction licenseStaticLimitRestriction = (LicenseStaticLimitRestriction) restriction;

    long limit = getLimit(licenseStaticLimitRestriction, accountIdentifier, featureDetailsDTO.getModuleType());
    long currentCount =
        getCurrentCount(featureRestrictionName, licenseStaticLimitRestriction, accountIdentifier, limit);

    featureDetailsDTO.setRestrictionType(licenseStaticLimitRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(
        LicenseStaticLimitRestrictionDTO.builder().limit(limit).count(currentCount).build());
    featureDetailsDTO.setAllowed(true);
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    return generateMetadataDTO(restriction, accountIdentifier, moduleType);
  }

  private LicenseStaticLimitRestrictionMetadataDTO generateMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    LicenseStaticLimitRestriction licenseStaticLimitRestriction = (LicenseStaticLimitRestriction) restriction;

    LicenseStaticLimitRestrictionMetadataDTO result =
        LicenseStaticLimitRestrictionMetadataDTO.builder()
            .restrictionType(licenseStaticLimitRestriction.getRestrictionType())
            .fieldName(licenseStaticLimitRestriction.getFieldName())
            .build();
    // In case of FE queries all metadata, skip the limit as it account dependent
    if (accountIdentifier != null && moduleType != null) {
      result.setLimit(getLimit(licenseStaticLimitRestriction, accountIdentifier, moduleType));
    }

    return result;
  }

  private long getCurrentCount(FeatureRestrictionName featureRestrictionName, LicenseStaticLimitRestriction restriction,
      String accountIdentifier, long limit) {
    StaticLimitRestrictionMetadataDTO metadataDTO =
        StaticLimitRestrictionMetadataDTO.builder().restrictionType(RestrictionType.STATIC_LIMIT).limit(limit).build();
    return RestrictionUtils.getCurrentUsage(restriction, featureRestrictionName, accountIdentifier, metadataDTO);
  }

  private long getLimit(LicenseLimitInterface licenseLimitInterface, String accountIdentifier, ModuleType moduleType) {
    LicensesWithSummaryDTO licenseSummary = licenseService.getLicenseSummary(accountIdentifier, moduleType);
    String fieldName = licenseLimitInterface.getFieldName();
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
