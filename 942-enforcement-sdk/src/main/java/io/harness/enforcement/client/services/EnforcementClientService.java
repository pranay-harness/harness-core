package io.harness.enforcement.client.services;

import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.EnforcementServiceConnectionException;
import io.harness.enforcement.exceptions.WrongFeatureStateException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface EnforcementClientService {
  boolean isAvailable(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  void checkAvailability(FeatureRestrictionName featureRestrictionName, String accountIdentifier);
  Map<FeatureRestrictionName, Boolean> getAvailabilityMap(
      Set<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier);
  Optional<RestrictionMetadataDTO> getRestrictionMetadata(FeatureRestrictionName featureRestrictionName,
      String accountIdentifier) throws WrongFeatureStateException, EnforcementServiceConnectionException;
  Map<FeatureRestrictionName, RestrictionMetadataDTO> getRestrictionMetadataMap(
      List<FeatureRestrictionName> featureRestrictionNames, String accountIdentifier)
      throws WrongFeatureStateException, EnforcementServiceConnectionException;
}
