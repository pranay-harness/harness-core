package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;

public class EnableDisableRestriction extends Restriction {
  private boolean enabled;

  public EnableDisableRestriction(RestrictionType restrictionType, boolean enabled) {
    super(restrictionType);
    this.enabled = enabled;
  }

  @Override
  public boolean check(String accountIdentifier) {
    return enabled;
  }
}
