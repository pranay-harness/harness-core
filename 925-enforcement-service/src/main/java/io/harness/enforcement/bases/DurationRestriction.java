package io.harness.enforcement.bases;

import io.harness.enforcement.beans.TimeUnit;
import io.harness.enforcement.constants.RestrictionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DurationRestriction extends Restriction {
  private TimeUnit timeUnit;

  public DurationRestriction(RestrictionType restrictionType, TimeUnit timeUnit) {
    super(restrictionType);
    this.timeUnit = timeUnit;
  }
}
