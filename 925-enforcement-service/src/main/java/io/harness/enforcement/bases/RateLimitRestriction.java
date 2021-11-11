package io.harness.enforcement.bases;

import io.harness.enforcement.beans.TimeUnit;
import io.harness.enforcement.constants.LimitSource;
import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.interfaces.LimitRestrictionInterface;
import io.harness.enforcement.services.impl.EnforcementSdkClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitRestriction extends Restriction implements LimitRestrictionInterface {
  Long limit;
  TimeUnit timeUnit;
  String clientName;
  boolean allowedIfEqual;
  EnforcementSdkClient enforcementSdkClient;
  LimitSource limitSource;
  String fieldName;
  boolean blockIfExceed;

  public RateLimitRestriction(RestrictionType restrictionType, long limit, TimeUnit timeUnit, boolean allowedIfEqual,
      EnforcementSdkClient enforcementSdkClient, LimitSource limitSource, String fieldName, boolean blockIfExceed) {
    super(restrictionType);
    this.limit = limit;
    this.timeUnit = timeUnit;
    this.allowedIfEqual = allowedIfEqual;
    this.enforcementSdkClient = enforcementSdkClient;
    this.limitSource = limitSource;
    this.fieldName = fieldName;
    this.blockIfExceed = blockIfExceed;
  }

  @Override
  public void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient) {
    this.enforcementSdkClient = enforcementSdkClient;
  }
}
