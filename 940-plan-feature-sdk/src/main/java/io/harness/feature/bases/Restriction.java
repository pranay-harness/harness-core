package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.RestrictionInterface;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class Restriction implements RestrictionInterface {
  protected RestrictionType restrictionType;

  @Override
  public RestrictionType getRestrictionType() {
    return restrictionType;
  }
}
