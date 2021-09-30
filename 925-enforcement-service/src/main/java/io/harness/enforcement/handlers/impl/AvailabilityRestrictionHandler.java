package io.harness.enforcement.handlers.impl;

import io.harness.enforcement.bases.AvailabilityRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.AvailabilityRestrictionDTO;
import io.harness.enforcement.beans.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.AvailabilityRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.exceptions.FeatureNotSupportedException;
import io.harness.enforcement.handlers.RestrictionHandler;

import com.google.inject.Singleton;

@Singleton
public class AvailabilityRestrictionHandler implements RestrictionHandler {
  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier) {
    checkWithUsage(featureRestrictionName, restriction, accountIdentifier, 0);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount) {
    AvailabilityRestriction availabilityRestriction = (AvailabilityRestriction) restriction;
    if (!availabilityRestriction.getEnabled()) {
      throw new FeatureNotSupportedException("Feature is not enabled");
    }
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    AvailabilityRestriction availabilityRestriction = (AvailabilityRestriction) restriction;

    featureDetailsDTO.setRestrictionType(availabilityRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(
        AvailabilityRestrictionDTO.builder().enabled(availabilityRestriction.getEnabled()).build());
    featureDetailsDTO.setAllowed(availabilityRestriction.getEnabled());
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(Restriction restriction) {
    AvailabilityRestriction availabilityRestriction = (AvailabilityRestriction) restriction;
    return AvailabilityRestrictionMetadataDTO.builder()
        .restrictionType(availabilityRestriction.getRestrictionType())
        .enabled(availabilityRestriction.getEnabled())
        .build();
  }
}
