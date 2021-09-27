package io.harness.enforcement.handlers;

import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;

public interface RestrictionHandler {
  void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier);
  void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      long currentCount);
  void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, FeatureRestrictionDetailsDTO featureDetailsDTO);
  RestrictionMetadataDTO getMetadataDTO(Restriction restriction);
}
