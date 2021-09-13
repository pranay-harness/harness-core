package io.harness.feature.services.impl;

import io.harness.ModuleType;
import io.harness.exception.InvalidRequestException;
import io.harness.feature.bases.Feature;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.cache.LicenseInfoCache;
import io.harness.feature.configs.FeatureCollection;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.FeatureNotSupportedException;
import io.harness.feature.handlers.RestrictionHandler;
import io.harness.feature.handlers.RestrictionHandlerFactory;
import io.harness.feature.services.FeatureService;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class FeatureServiceImpl implements FeatureService {
  private Map<String, Feature> featureMap;
  private final LicenseInfoCache licenseInfoCache;
  private final RestrictionHandlerFactory restrictionHandlerFactory;

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
    handler.check(accountIdentifier);
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
  public List<String> getAllFeatureNames() {
    return Arrays.stream(FeatureCollection.values()).map(FeatureCollection::name).collect(Collectors.toList());
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
}
