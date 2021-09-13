package io.harness.feature.services.impl;

import io.harness.ModuleType;
import io.harness.exception.InvalidRequestException;
import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.bases.Feature;
import io.harness.feature.bases.Restriction;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.cache.LicenseInfoCache;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.FeatureNotSupportedException;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandler;
import io.harness.feature.handlers.RestrictionHandlerFactory;
import io.harness.feature.interfaces.LimitRestriction;
import io.harness.feature.services.FeatureService;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class FeatureServiceImpl implements FeatureService {
  private Map<String, Feature> featureMap;
  private final LicenseInfoCache licenseInfoCache;
  private final RestrictionHandlerFactory restrictionHandlerFactory;

  private static final String COLON = ":";
  private static final String SEMI_COLON = ";";
  private static final String UPGRADE_PLAN = ". Plan to upgrade: ";
  private static final String MESSAGE_PARAM = "message";

  @Inject
  public FeatureServiceImpl(LicenseInfoCache licenseInfoCache, RestrictionHandlerFactory restrictionHandlerFactory) {
    featureMap = new HashMap<>();
    this.licenseInfoCache = licenseInfoCache;
    this.restrictionHandlerFactory = restrictionHandlerFactory;
  }

  void registerFeature(String featureName, Feature feature) {
    if (featureMap.containsKey(featureName)) {
      throw new IllegalArgumentException(String.format("Feature [%s] has been registered", featureName));
    }
    featureMap.put(featureName, feature);
  }

  @Override
  public boolean isFeatureAvailable(String featureName, String accountIdentifier) {
    try {
      checkAvailabilityOrThrow(featureName, accountIdentifier);
      return true;
    } catch (Exception e) {
      log.debug(String.format("feature [%s] is not available for account [%s]", featureName, accountIdentifier), e);
      return false;
    }
  }

  @Override
  public void checkAvailabilityOrThrow(String featureName, String accountIdentifier) {
    if (!isFeatureDefined(featureName)) {
      throw new FeatureNotSupportedException(String.format("Feature [%s] is not defined", featureName));
    }
    Feature feature = featureMap.get(featureName);
    LicensesWithSummaryDTO licenseInfo = getLicenseInfo(accountIdentifier, feature.getModuleType());
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(feature, licenseInfo.getEdition());
    try {
      handler.check(accountIdentifier);
    } catch (FeatureNotSupportedException e) {
      String message = (String) e.getParams().get(MESSAGE_PARAM);
      e.getParams().put(
          MESSAGE_PARAM, generateSuggestionMessage(message, feature, licenseInfo.getEdition(), "enabled"));
      throw e;
    } catch (LimitExceededException le) {
      String message = (String) le.getParams().get(MESSAGE_PARAM);
      le.getParams().put(
          MESSAGE_PARAM, generateSuggestionMessage(message, feature, licenseInfo.getEdition(), "unlimited"));
      throw le;
    }
  }

  @Override
  public FeatureDetailsDTO getFeatureDetail(String featureName, String accountIdentifier) {
    if (!isFeatureDefined(featureName)) {
      throw new InvalidRequestException(String.format("Feature [%s] is not defined", featureName));
    }
    Feature feature = featureMap.get(featureName);
    LicensesWithSummaryDTO licenseInfo = getLicenseInfo(accountIdentifier, feature.getModuleType());
    return toFeatureDetailsDTO(accountIdentifier, feature, licenseInfo);
  }

  @Override
  public List<FeatureDetailsDTO> getEnabledFeatureDetails(String accountIdentifier, ModuleType moduleType) {
    List<FeatureDetailsDTO> result = new ArrayList<>();
    for (Feature feature : featureMap.values()) {
      if (feature.getModuleType().equals(moduleType)) {
        LicensesWithSummaryDTO licenseInfo = getLicenseInfo(accountIdentifier, feature.getModuleType());
        RestrictionType restrictionType = feature.getRestrictions().get(licenseInfo.getEdition()).getRestrictionType();
        if (RestrictionType.ENABLED.equals(restrictionType)) {
          result.add(toFeatureDetailsDTO(accountIdentifier, feature, licenseInfo));
        }
      }
    }
    return result;
  }

  @Override
  public Set<String> getAllFeatureNames() {
    return featureMap.keySet();
  }

  @Override
  public boolean isLockRequired(String featureName, String accountIdentifier) {
    if (!isFeatureDefined(featureName)) {
      throw new InvalidRequestException(String.format("Feature [%s] is not defined", featureName));
    }
    Feature feature = featureMap.get(featureName);
    LicensesWithSummaryDTO licenseInfo = getLicenseInfo(accountIdentifier, feature.getModuleType());
    RestrictionType restrictionType = feature.getRestrictions().get(licenseInfo.getEdition()).getRestrictionType();
    return RestrictionType.RATE_LIMIT.equals(restrictionType) || RestrictionType.STATIC_LIMIT.equals(restrictionType);
  }

  private boolean isFeatureDefined(String featureName) {
    return featureMap.containsKey(featureName);
  }

  private boolean isLicenseValid(LicensesWithSummaryDTO licenseInfo) {
    return licenseInfo != null && licenseInfo.getMaxExpiryTime() > Instant.now().toEpochMilli();
  }

  private LicensesWithSummaryDTO getLicenseInfo(String accountIdentifier, ModuleType moduleType) {
    LicensesWithSummaryDTO licenseInfo = licenseInfoCache.getLicenseInfo(accountIdentifier, moduleType);
    if (!isLicenseValid(licenseInfo)) {
      throw new FeatureNotSupportedException("Invalid license status");
    }
    return licenseInfo;
  }

  private FeatureDetailsDTO toFeatureDetailsDTO(
      String accountIdentifier, Feature feature, LicensesWithSummaryDTO licenseInfo) {
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(feature, licenseInfo.getEdition());
    RestrictionDTO restrictionDTO = handler.toRestrictionDTO(accountIdentifier);
    return FeatureDetailsDTO.builder()
        .name(feature.getName())
        .moduleType(feature.getModuleType().name())
        .description(feature.getDescription())
        .restriction(restrictionDTO)
        .build();
  }

  private String generateSuggestionMessage(String message, Feature feature, Edition edition, String enableDefinition) {
    StringBuilder suggestionMessage = new StringBuilder();
    suggestionMessage.append(message).append(UPGRADE_PLAN);
    List<Edition> superiorEditions = Edition.getSuperiorEdition(edition);
    for (Edition superiorEdition : superiorEditions) {
      Restriction restriction = feature.getRestrictions().get(superiorEdition);
      if (RestrictionType.ENABLED.equals(restriction.getRestrictionType())) {
        EnableDisableRestriction enableDisableRestriction = (EnableDisableRestriction) restriction;
        if (enableDisableRestriction.isEnabled()) {
          suggestionMessage.append(superiorEdition.name()).append(COLON).append(enableDefinition).append(SEMI_COLON);
        }
      } else if (restriction instanceof LimitRestriction) {
        LimitRestriction limitRestriction = (LimitRestriction) restriction;
        suggestionMessage.append(superiorEdition.name())
            .append(COLON)
            .append(limitRestriction.getLimit())
            .append(SEMI_COLON);
      }
    }
    return suggestionMessage.toString();
  }
}
