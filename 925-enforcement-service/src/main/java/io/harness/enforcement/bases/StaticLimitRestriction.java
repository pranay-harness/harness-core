package io.harness.enforcement.bases;

import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.interfaces.LimitRestrictionInterface;
import io.harness.enforcement.services.impl.EnforcementSdkClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StaticLimitRestriction extends Restriction implements LimitRestrictionInterface {
  Long limit;
  String clientName;
  boolean allowedIfEqual;
  EnforcementSdkClient enforcementSdkClient;

  public StaticLimitRestriction(
      RestrictionType restrictionType, long limit, boolean allowedIfEqual, EnforcementSdkClient enforcementSdkClient) {
    super(restrictionType);
    this.limit = limit;
    this.allowedIfEqual = allowedIfEqual;
    this.enforcementSdkClient = enforcementSdkClient;
  }

  @Override
  public void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient) {
    this.enforcementSdkClient = enforcementSdkClient;
  }
}