package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.StaticLimitInterface;

import lombok.Getter;

@Getter
public class StaticLimitRestriction extends Restriction {
  long limit;
  StaticLimitInterface staticLimitInterface;

  public StaticLimitRestriction(
      RestrictionType restrictionType, long limit, StaticLimitInterface staticLimitInterface) {
    super(restrictionType);
    this.limit = limit;
    this.staticLimitInterface = staticLimitInterface;
  }
}
