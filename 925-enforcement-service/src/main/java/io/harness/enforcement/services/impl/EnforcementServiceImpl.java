package io.harness.enforcement.services.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.AvailabilityRestriction;
import io.harness.enforcement.bases.FeatureRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.internal.RestrictionMetadataMapResponseDTO;
import io.harness.enforcement.beans.metadata.FeatureRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.exceptions.FeatureNotSupportedException;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.enforcement.handlers.RestrictionHandlerFactory;
import io.harness.enforcement.services.EnforcementService;
import io.harness.exception.InvalidRequestException;
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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class EnforcementServiceImpl implements EnforcementService {
  private final Map<FeatureRestrictionName, FeatureRestriction> featureRestrictionMap;
  private final io.harness.enforcement.handlers.RestrictionHandlerFactory restrictionHandlerFactory;
  private final LicenseService licenseService;

  @Inject
  public EnforcementServiceImpl(LicenseService licenseService, RestrictionHandlerFactory restrictionHandlerFactory) {
    featureRestrictionMap = new HashMap<>();
    this.licenseService = licenseService;
    this.restrictionHandlerFactory = restrictionHandlerFactory;
  }

  void registerFeature(FeatureRestrictionName featureRestrictionName, FeatureRestriction feature) {
    if (featureRestrictionMap.containsKey(featureRestrictionName)) {
      throw new IllegalArgumentException(String.format("Feature [%s] has been registered", featureRestrictionName));
    }
    featureRestrictionMap.put(featureRestrictionName, feature);
  }

  @Override
  public boolean isFeatureAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier) {
    try {
      checkAvailabilityOrThrow(featureRestrictionName, accountIdentifier);
      return true;
    } catch (Exception e) {
      log.debug(
          String.format("feature [%s] is not available for account [%s]", featureRestrictionName, accountIdentifier),
          e);
      return false;
    }
  }

  @Override
  public void checkAvailabilityOrThrow(FeatureRestrictionName featureRestrictionName, String accountIdentifier) {
    if (!isFeatureRestrictionDefined(featureRestrictionName)) {
      throw new FeatureNotSupportedException(String.format("Feature [%s] is not defined", featureRestrictionName));
    }
    FeatureRestriction feature = featureRestrictionMap.get(featureRestrictionName);
    Edition edition = getLicenseEdition(accountIdentifier, feature.getModuleType());

    Restriction restriction = getRestriction(feature, edition);
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(restriction.getRestrictionType());

    handler.check(featureRestrictionName, restriction, accountIdentifier, feature.getModuleType(), edition);
  }

  @Override
  public FeatureRestrictionMetadataDTO getFeatureMetadata(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier) {
    if (!isFeatureRestrictionDefined(featureRestrictionName)) {
      throw new InvalidRequestException(String.format("Feature [%s] is not defined", featureRestrictionName));
    }
    FeatureRestriction featureRestriction = featureRestrictionMap.get(featureRestrictionName);
    Edition edition = getLicenseEdition(accountIdentifier, featureRestriction.getModuleType());
    return toFeatureMetadataDTO(featureRestriction, edition);
  }

  @Override
  public RestrictionMetadataMapResponseDTO getFeatureRestrictionMetadataMap(
      List<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier) {
    Map<FeatureRestrictionName, FeatureRestrictionMetadataDTO> metadataDTOMap = new HashMap<>();

    for (FeatureRestrictionName name : featureRestrictionNames) {
      FeatureRestriction featureRestriction = featureRestrictionMap.get(name);
      Edition edition = getLicenseEdition(accountIdentifier, featureRestriction.getModuleType());

      FeatureRestrictionMetadataDTO featureRestrictionMetadataDTO = toFeatureMetadataDTO(featureRestriction, edition);
      metadataDTOMap.put(name, featureRestrictionMetadataDTO);
    }
    return RestrictionMetadataMapResponseDTO.builder().metadataMap(metadataDTOMap).build();
  }

  @Override
  public List<FeatureRestrictionMetadataDTO> getAllFeatureRestrictionMetadata() {
    return featureRestrictionMap.values()
        .stream()
        .map(featureRestriction -> toFeatureMetadataDTO(featureRestriction, null))
        .collect(Collectors.toList());
  }

  @Override
  public FeatureRestrictionDetailsDTO getFeatureDetail(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier) {
    if (!isFeatureRestrictionDefined(featureRestrictionName)) {
      throw new InvalidRequestException(String.format("Feature [%s] is not defined", featureRestrictionName));
    }
    FeatureRestriction featureRestriction = featureRestrictionMap.get(featureRestrictionName);
    Edition edition = getLicenseEdition(accountIdentifier, featureRestriction.getModuleType());
    return toFeatureDetailsDTO(accountIdentifier, featureRestriction, edition);
  }

  @Override
  public List<FeatureRestrictionDetailsDTO> getEnabledFeatureDetails(String accountIdentifier) {
    List<FeatureRestrictionDetailsDTO> result = new ArrayList<>();
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

      for (FeatureRestriction featureRestriction : featureRestrictionMap.values()) {
        if (featureRestriction.getModuleType().equals(moduleType)) {
          Restriction restriction = featureRestriction.getRestrictions().get(edition);
          if (isEnabledFeature(restriction)) {
            result.add(toFeatureDetailsDTO(accountIdentifier, featureRestriction, edition));
          }
        }
      }
    }
    return result;
  }

  private boolean isEnabledFeature(Restriction restriction) {
    if (RestrictionType.AVAILABILITY.equals(restriction.getRestrictionType())) {
      AvailabilityRestriction enableDisableRestriction = (AvailabilityRestriction) restriction;
      if (enableDisableRestriction.getEnabled()) {
        return true;
      }
    }
    return false;
  }

  private boolean isFeatureRestrictionDefined(FeatureRestrictionName featureRestrictionName) {
    return featureRestrictionMap.containsKey(featureRestrictionName);
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

  private FeatureRestrictionDetailsDTO toFeatureDetailsDTO(
      String accountIdentifier, FeatureRestriction feature, Edition edition) {
    Restriction restriction = getRestriction(feature, edition);
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(restriction.getRestrictionType());
    FeatureRestrictionDetailsDTO featureDetailsDTO = FeatureRestrictionDetailsDTO.builder()
                                                         .name(feature.getName())
                                                         .moduleType(feature.getModuleType())
                                                         .description(feature.getDescription())
                                                         .build();
    handler.fillRestrictionDTO(feature.getName(), restriction, accountIdentifier, edition, featureDetailsDTO);
    return featureDetailsDTO;
  }

  private FeatureRestrictionMetadataDTO toFeatureMetadataDTO(FeatureRestriction feature, Edition edition) {
    FeatureRestrictionMetadataDTO featureDetailsDTO = FeatureRestrictionMetadataDTO.builder()
                                                          .name(feature.getName())
                                                          .moduleType(feature.getModuleType())
                                                          .edition(edition)
                                                          .build();

    Map<Edition, RestrictionMetadataDTO> restrictionMetadataDTOMap =
        feature.getRestrictions().entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, entry -> toRestrictionMetadataDTO(entry.getValue())));
    featureDetailsDTO.setRestrictionMetadata(restrictionMetadataDTOMap);
    return featureDetailsDTO;
  }

  private RestrictionMetadataDTO toRestrictionMetadataDTO(Restriction restriction) {
    RestrictionHandler handler = restrictionHandlerFactory.getHandler(restriction.getRestrictionType());
    return handler.getMetadataDTO(restriction);
  }

  private Restriction getRestriction(FeatureRestriction feature, Edition edition) {
    Restriction restriction = feature.getRestrictions().get(edition);
    if (restriction == null) {
      throw new FeatureNotSupportedException("Invalid feature definition");
    }
    return restriction;
  }
}
