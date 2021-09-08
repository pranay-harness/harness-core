package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.StaticLimitInterface;

public class StaticLimitRestriction extends Restriction {
  long limit;
  StaticLimitInterface staticLimit;

  public StaticLimitRestriction(RestrictionType restrictionType, long limit, StaticLimitInterface staticLimit) {
    super(restrictionType);
    this.limit = limit;
    this.staticLimit = staticLimit;
  }

  @Override
  public boolean check(String accountIdentifier) {
    return staticLimit.getCurrentValue(accountIdentifier) < limit;
  }
}
