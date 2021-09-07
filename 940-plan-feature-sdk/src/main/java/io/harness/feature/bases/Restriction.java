package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class Restriction {
  protected RestrictionType restrictionType;

  public RestrictionType getRestrictionType() {
    return restrictionType;
  }
}
