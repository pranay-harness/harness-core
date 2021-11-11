package io.harness.enforcement.handlers.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.LicenseLimitRestriction;
import io.harness.enforcement.bases.RateLimitRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.details.RateLimitRestrictionDTO;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.constants.LimitSource;
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

@Singleton
public class RateLimitRestrictionHandler implements RestrictionHandler {
  private final LicenseService licenseService;

  @Inject
  public RateLimitRestrictionHandler(LicenseService licenseService) {
    this.licenseService = licenseService;
  }

  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    if (LimitSource.LICENSE.equals(rateLimitRestriction.getLimitSource()) && !rateLimitRestriction.isBlockIfExceed()) {
      return;
    }

    long currentCount = getCurrentCount(featureRestrictionName, rateLimitRestriction, accountIdentifier);
    checkWithUsage(featureRestrictionName, rateLimitRestriction, accountIdentifier, currentCount, moduleType, edition);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount, ModuleType moduleType, Edition edition) {
    RateLimitRestriction rateLimitRestriction = (RateLimitRestriction) restriction;
    if (LimitSource.LICENSE.equals(rateLimitRestriction.getLimitSource()) && !rateLimitRestriction.isBlockIfExceed()) {
      return;
    }

    long limit = getLimit(rateLimitRestriction, accountIdentifier, moduleType);
    if (!RestrictionUtils.isAvailable(currentCount, limit, rateLimitRestriction.isAllowedIfEqual())) {
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

  private long getLimit(RateLimitRestriction restriction, String accountIdentifier, ModuleType moduleType) {
    if (LimitSource.LICENSE.equals(restriction.getLimitSource()){
      return getLicenseLimit(restriction, accountIdentifier, moduleType);
    }else{
      return restriction.getLimit();
    }
  }

  private long getLicenseLimit(RateLimitRestriction restriction, String accountIdentifier, ModuleType moduleType) {
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
