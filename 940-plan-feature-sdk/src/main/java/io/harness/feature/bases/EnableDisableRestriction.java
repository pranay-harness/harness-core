package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;

import lombok.Getter;

@Getter
public class EnableDisableRestriction extends Restriction {
  private boolean enabled;

  public EnableDisableRestriction(RestrictionType restrictionType, boolean enabled) {
    super(restrictionType);
    this.enabled = enabled;
  }
}
