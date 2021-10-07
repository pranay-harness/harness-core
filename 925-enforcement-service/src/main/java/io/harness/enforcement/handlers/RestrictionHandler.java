package io.harness.enforcement.handlers;

import io.harness.ModuleType;
import io.harness.enforcement.bases.Restriction;
import io.harness.enforcement.beans.details.FeatureRestrictionDetailsDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.licensing.Edition;

public interface RestrictionHandler {
  void check(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      ModuleType moduleType, Edition edition);
  void checkWithUsage(FeatureRestrictionName featureRestrictionName, Restriction restriction, String accountIdentifier,
      long currentCount, ModuleType moduleType, Edition edition);
  void fillRestrictionDTO(FeatureRestrictionName featureRestrictionName, Restriction restriction,
      String accountIdentifier, Edition edition, FeatureRestrictionDetailsDTO featureDetailsDTO);
  RestrictionMetadataDTO getMetadataDTO(Restriction restriction);
}
