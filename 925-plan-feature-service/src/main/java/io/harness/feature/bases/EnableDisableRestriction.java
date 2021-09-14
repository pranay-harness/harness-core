package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnableDisableRestriction extends Restriction {
  private Boolean enabled;

  public EnableDisableRestriction(RestrictionType restrictionType, boolean enabled) {
    super(restrictionType);
    this.enabled = enabled;
  }
}
