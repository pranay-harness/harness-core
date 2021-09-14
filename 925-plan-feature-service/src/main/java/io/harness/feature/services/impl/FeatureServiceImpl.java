package io.harness.feature.services.impl;

import io.harness.ModuleType;
import io.harness.exception.InvalidRequestException;
import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.bases.Feature;
import io.harness.feature.bases.Restriction;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.FeatureNotSupportedException;
import io.harness.feature.exceptions.LimitExceededException;
import io.harness.feature.handlers.RestrictionHandler;
import io.harness.feature.handlers.RestrictionHandlerFactory;
import io.harness.feature.interfaces.LimitRestriction;
import io.harness.feature.services.FeatureService;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.services.LicenseService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class FeatureServiceImpl implements FeatureService {
  private final Map<FeatureRestriction, Feature> featureMap;
  private final RestrictionHandlerFactory restrictionHandlerFactory;
  private final LicenseService licenseService;

  private static final String COLON = ":";
  private static final String SEMI_COLON = ";";
  private static final String UPGRADE_PLAN = ". Plan to upgrade: ";
  private static final String MESSAGE_PARAM = "message";

  @Inject
  public FeatureServiceImpl(LicenseService licenseService, RestrictionHandlerFactory restrictionHandlerFactory) {
    featureMap = new HashMap<>();
    this.licenseService = licenseService;
    this.restrictionHandlerFactory = restrictionHandlerFactory;
  }

  void registerFeature(FeatureRestriction featureName, Feature feature) {
    if (featureMap.containsKey(featureName)) {
      throw new IllegalArgumentException(String.format("Feature [%s] has been registered", featureName));
    }
    featureMap.put(featureName, feature);
  }

  @Override
  public boolean isFeatureAvailable(FeatureRestriction featureName, String accountIdentifier) {
    try {
      checkAvailabilityOrThrow(featureName, accountIdentifier);
      return true;
    } catch (Exception e) {
      log.debug(String.format("feature [%s] is not available for account [%s]", featureName, accountIdentifier), e);
      return false;
    }
  }

  @Override
  public void checkAvailabilityOrThrow(FeatureRestriction featureName, String accountIdentifier) {
    if (!isFeatureDefined(featureName)) {
      throw new FeatureNotSupportedException(String.format("Feature [%s] is not defined", featureName));
    }
    Feature feature = featureMap.get(featureName);
    Edition edition = getLicenseEdition(accountIdentifier, feature.getModuleType());
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(feature, edition);
    try {
      handler.check(featureName, accountIdentifier);
    } catch (FeatureNotSupportedException e) {
      String message = (String) e.getParams().get(MESSAGE_PARAM);
      e.getParams().put(MESSAGE_PARAM, generateSuggestionMessage(message, feature, edition, "enabled"));
      throw e;
    } catch (LimitExceededException le) {
      String message = (String) le.getParams().get(MESSAGE_PARAM);
      le.getParams().put(MESSAGE_PARAM, generateSuggestionMessage(message, feature, edition, "unlimited"));
      throw le;
    }
  }

  @Override
  public void checkAvailabilityWithUsage(FeatureRestriction featureName, String accountIdentifier, long currentUsage) {
    if (!isFeatureDefined(featureName)) {
      throw new FeatureNotSupportedException(String.format("Feature [%s] is not defined", featureName));
    }
    Feature feature = featureMap.get(featureName);
    Edition edition = getLicenseEdition(accountIdentifier, feature.getModuleType());
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(feature, edition);
    try {
      handler.checkWithUsage(featureName, accountIdentifier, currentUsage);
    } catch (FeatureNotSupportedException e) {
      String message = (String) e.getParams().get(MESSAGE_PARAM);
      e.getParams().put(MESSAGE_PARAM, generateSuggestionMessage(message, feature, edition, "enabled"));
      throw e;
    } catch (LimitExceededException le) {
      String message = (String) le.getParams().get(MESSAGE_PARAM);
      le.getParams().put(MESSAGE_PARAM, generateSuggestionMessage(message, feature, edition, "unlimited"));
      throw le;
    }
  }

  @Override
  public FeatureDetailsDTO getFeatureDetail(FeatureRestriction featureName, String accountIdentifier) {
    if (!isFeatureDefined(featureName)) {
      throw new InvalidRequestException(String.format("Feature [%s] is not defined", featureName));
    }
    Feature feature = featureMap.get(featureName);
    Edition edition = getLicenseEdition(accountIdentifier, feature.getModuleType());
    return toFeatureDetailsDTO(accountIdentifier, feature, edition);
  }

  @Override
  public List<FeatureDetailsDTO> getEnabledFeatureDetails(String accountIdentifier) {
    List<FeatureDetailsDTO> result = new ArrayList<>();
    // check all valid module type features(CD,CCM,FF,CI,CORE)
    for (ModuleType moduleType : ModuleType.values()) {
      if (moduleType.isInternal() && !ModuleType.CORE.equals(moduleType)) {
        continue;
      }

      Edition edition;
      try {
        edition = getLicenseEdition(accountIdentifier, moduleType);
      } catch (FeatureNotSupportedException e) {
        log.debug("Not able to get license edition for [{}] under account [{}], ", accountIdentifier, moduleType);
        continue;
      }

      for (Feature feature : featureMap.values()) {
        if (feature.getModuleType().equals(moduleType)) {
          Restriction restriction = feature.getRestrictions().get(edition);
          if (isEnabledFeature(restriction)) {
            result.add(toFeatureDetailsDTO(accountIdentifier, feature, edition));
          }
        }
      }
    }
    return result;
  }

  private boolean isEnabledFeature(Restriction restriction) {
    if (RestrictionType.AVAILABILITY.equals(restriction.getRestrictionType())) {
      EnableDisableRestriction enableDisableRestriction = (EnableDisableRestriction) restriction;
      if (enableDisableRestriction.getEnabled()) {
        return true;
      }
    }
    return false;
  }

  private boolean isFeatureDefined(FeatureRestriction featureName) {
    return featureMap.containsKey(featureName);
  }

  private boolean isLicenseValid(LicensesWithSummaryDTO licenseInfo) {
    return licenseInfo != null && licenseInfo.getMaxExpiryTime() > Instant.now().toEpochMilli();
  }

  private Edition getLicenseEdition(String accountIdentifier, ModuleType moduleType) {
    // if PL feature edition check
    if (ModuleType.CORE.equals(moduleType)) {
      Edition edition = licenseService.calculateAccountEdition(accountIdentifier);
      if (edition == null) {
        throw new FeatureNotSupportedException("Invalid license status");
      }
      return edition;
    }

    // other module feature
    LicensesWithSummaryDTO licenseInfo = licenseService.getLicenseSummary(accountIdentifier, moduleType);
    if (!isLicenseValid(licenseInfo)) {
      throw new FeatureNotSupportedException("Invalid license status");
    }
    return licenseInfo.getEdition();
  }

  private FeatureDetailsDTO toFeatureDetailsDTO(String accountIdentifier, Feature feature, Edition edition) {
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(feature, edition);
    FeatureDetailsDTO featureDetailsDTO = FeatureDetailsDTO.builder()
                                              .name(feature.getName())
                                              .moduleType(feature.getModuleType().name())
                                              .description(feature.getDescription())
                                              .build();
    handler.fillRestrictionDTO(feature.getName(), accountIdentifier, featureDetailsDTO);
    return featureDetailsDTO;
  }

  private String generateSuggestionMessage(String message, Feature feature, Edition edition, String enableDefinition) {
    StringBuilder suggestionMessage = new StringBuilder();
    suggestionMessage.append(message).append(UPGRADE_PLAN);
    List<Edition> superiorEditions = Edition.getSuperiorEdition(edition);
    for (Edition superiorEdition : superiorEditions) {
      Restriction restriction = feature.getRestrictions().get(superiorEdition);
      if (RestrictionType.AVAILABILITY.equals(restriction.getRestrictionType())) {
        EnableDisableRestriction enableDisableRestriction = (EnableDisableRestriction) restriction;
        if (enableDisableRestriction.getEnabled()) {
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
