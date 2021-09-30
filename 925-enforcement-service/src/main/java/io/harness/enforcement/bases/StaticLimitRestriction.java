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
  EnforcementSdkClient enforcementSdkClient;

  public StaticLimitRestriction(
      RestrictionType restrictionType, long limit, EnforcementSdkClient enforcementSdkClient) {
    super(restrictionType);
    this.limit = limit;
    this.enforcementSdkClient = enforcementSdkClient;
  }

  public void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient) {
    this.enforcementSdkClient = enforcementSdkClient;
  }
}