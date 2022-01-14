package io.harness.enforcement.bases;

import io.harness.enforcement.constants.RestrictionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRestriction extends Restriction {
  private Boolean enabled;

  public AvailabilityRestriction(RestrictionType restrictionType, boolean enabled) {
    super(restrictionType);
    this.enabled = enabled;
  }
}
