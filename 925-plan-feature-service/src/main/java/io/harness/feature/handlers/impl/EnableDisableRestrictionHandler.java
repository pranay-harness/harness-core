package io.harness.feature.handlers.impl;

import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.beans.EnableDisableRestrictionDTO;
import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.FeatureNotSupportedException;
import io.harness.feature.handlers.RestrictionHandler;

public class EnableDisableRestrictionHandler implements RestrictionHandler {
  private final EnableDisableRestriction enableDisableRestriction;

  public EnableDisableRestrictionHandler(EnableDisableRestriction enableDisableRestriction) {
    this.enableDisableRestriction = enableDisableRestriction;
  }

  @Override
  public RestrictionType getRestrictionType() {
    return enableDisableRestriction.getRestrictionType();
  }

  @Override
  public void check(FeatureRestriction featureName, String accountIdentifier) {
    checkWithUsage(featureName, accountIdentifier, 0);
  }

  @Override
  public void checkWithUsage(FeatureRestriction featureName, String accountIdentifier, long currentCount) {
    if (!enableDisableRestriction.getEnabled()) {
      throw new FeatureNotSupportedException("Feature is not enabled");
    }
  }

  @Override
  public void fillRestrictionDTO(
      FeatureRestriction featureName, String accountIdentifier, FeatureDetailsDTO featureDetailsDTO) {
    featureDetailsDTO.setRestrictionType(enableDisableRestriction.getRestrictionType());
    featureDetailsDTO.setRestriction(
        EnableDisableRestrictionDTO.builder().enabled(enableDisableRestriction.getEnabled()).build());
    featureDetailsDTO.setAllowed(enableDisableRestriction.getEnabled());
  }
}
