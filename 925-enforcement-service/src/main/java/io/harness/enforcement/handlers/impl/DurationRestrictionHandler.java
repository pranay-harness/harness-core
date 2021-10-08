package io.harness.enforcement.handlers.impl;

import io.harness.ModuleType;
import io.harness.enforcement.bases.DurationRestriction;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.DurationRestrictionDTO;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.DurationRestrictionMetadataDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.handlers.RestrictionHandler;
import io.harness.licensing.Edition;

import com.google.inject.Singleton;

@Singleton
public class DurationRestrictionHandler implements RestrictionHandler {
  @Override
  public void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition) {
    checkWithUsage(featureRestrictionName, restriction, accountIdentifier, 0, moduleType, edition);
  }

  @Override
  public void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, long currentCount, ModuleType moduleType, Edition edition) {
    // Do nothing for duration type
  }

  @Override
  public void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, Edition edition, FeatureRestrictionDetailsDTO featureDetailsDTO) {
    DurationRestriction durationRestriction = (DurationRestriction) restriction;
    featureDetailsDTO.setAllowed(true);
    featureDetailsDTO.setRestrictionType(durationRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(
        DurationRestrictionDTO.builder().timeUnit(durationRestriction.getTimeUnit()).build());
  }

  @Override
  public RestrictionMetadataDTO getMetadataDTO(Restriction restriction) {
    DurationRestriction durationRestriction = (DurationRestriction) restriction;
    return DurationRestrictionMetadataDTO.builder()
        .restrictionType(durationRestriction.getRestrictionType())
        .timeUnit(durationRestriction.getTimeUnit())
        .build();
  }
}
