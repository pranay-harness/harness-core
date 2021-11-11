package io.harness.enforcement.handlers.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.LicenseLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.details.LicenseLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.LicenseLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.LimitExceededException;
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
public class LicenseLimitRestrictionHandler implements RestrictionHandler {
  private final LicenseService licenseService;

  @Inject
  public LicenseLimitRestrictionHandler(LicenseService licenseService) {
    this.licenseService = licenseService;
  }

  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition) {
    LicenseLimitRestriction licenseResourceRestriction = (LicenseLimitRestriction) restriction;

    if (licenseResourceRestriction.isBlockIfExceed()) {
      long currentCount =
          getCurrentCount(featureRestrictionName, licenseResourceRestriction, accountIdentifier, moduleType);
      checkWithUsage(featureRestrictionName, restriction, accountIdentifier, currentCount, moduleType, edition);
    }
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount, ModuleType moduleType, Edition edition) {
    LicenseLimitRestriction licenseResourceRestriction = (LicenseLimitRestriction) restriction;

    if (licenseResourceRestriction.isBlockIfExceed()) {
      long limit = getLimit(licenseResourceRestriction, accountIdentifier, moduleType);
      if (!RestrictionUtils.isAvailable(currentCount, limit, true)) {
        throw new LimitExceededException(String.format("Exceeded rate limitation. Current Limit: %s", limit));
      }
    }
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, Edition edition, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    LicenseLimitRestriction licenseResourceRestriction = (LicenseLimitRestriction) restriction;
    long currentCount = getCurrentCount(
        featureRestrictionName, licenseResourceRestriction, accountIdentifier, featureDetailsDTO.getModuleType());
    long limit = getLimit(licenseResourceRestriction, accountIdentifier, featureDetailsDTO.getModuleType());

    featureDetailsDTO.setRestrictionType(licenseResourceRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(LicenseLimitRestrictionDTO.builder()
                                         .limit(limit)
                                         .count(currentCount)
                                         .blockIfExceed(licenseResourceRestriction.isBlockIfExceed())
                                         .build());

    if (licenseResourceRestriction.isBlockIfExceed()) {
      featureDetailsDTO.setAllowed(RestrictionUtils.isAvailable(currentCount, limit, true));
    } else {
      featureDetailsDTO.setAllowed(true);
    }
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    return generateMetadataDTO(restriction, accountIdentifier, moduleType);
  }

  private long getLimit(LicenseLimitRestriction restriction, String accountIdentifier, ModuleType moduleType) {
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

  private LicenseLimitRestrictionMetadataDTO generateMetadataDTO(
      Restriction restriction, String accountIdentifier, ModuleType moduleType) {
    LicenseLimitRestriction licenseResourceRestriction = (LicenseLimitRestriction) restriction;

    LicenseLimitRestrictionMetadataDTO result = LicenseLimitRestrictionMetadataDTO.builder()
                                                    .blockIfExceed(licenseResourceRestriction.isBlockIfExceed())
                                                    .restrictionType(licenseResourceRestriction.getRestrictionType())
                                                    .fieldName(licenseResourceRestriction.getFieldName())
                                                    .build();
    // In case of FE queries all metadata, skip the limit as it account dependent
    if (accountIdentifier == null || moduleType == null) {
      result.setLimit(getLimit(licenseResourceRestriction, accountIdentifier, moduleType));
    }

    return result;
  }

  private long getCurrentCount(FeatureRestrictionName featureRestrictionName, LicenseLimitRestriction restriction,
      String accountIdentifier, ModuleType moduleType) {
    RestrictionMetadataDTO metadataDTO = generateMetadataDTO(restriction, accountIdentifier, moduleType);
    return RestrictionUtils.getCurrentUsage(restriction, featureRestrictionName, accountIdentifier, metadataDTO);
  }
}
