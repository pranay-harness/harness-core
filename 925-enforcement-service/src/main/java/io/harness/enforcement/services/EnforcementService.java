package io.harness.enforcement.services;

import io.harness.enforcement.beans.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.FeatureRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;

import java.util.List;

public interface EnforcementService {
  boolean isFeatureAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  void checkAvailabilityOrThrow(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  FeatureRestrictionMetadataDTO getFeatureMetadata(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  FeatureRestrictionDetailsDTO getFeatureDetail(
      FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  List<FeatureRestrictionDetailsDTO> getEnabledFeatureDetails(String accountIdentifier);
}
