package io.harness.enforcement.bases;

import io.harness.enforcement.constants.RestrictionType;
import io.harness.enforcement.interfaces.EnforcementSdkSupportInterface;
import io.harness.enforcement.interfaces.LicenseLimitInterface;
import io.harness.enforcement.services.impl.EnforcementSdkClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LicenseStaticLimitRestriction
    extends Restriction implements EnforcementSdkSupportInterface, LicenseLimitInterface {
  String fieldName;
  String clientName;
  EnforcementSdkClient enforcementSdkClient;

  public LicenseStaticLimitRestriction(
      RestrictionType restrictionType, String fieldName, EnforcementSdkClient enforcementSdkClient) {
    super(restrictionType);
    this.fieldName = fieldName;
    this.enforcementSdkClient = enforcementSdkClient;
  }

  @Override
  public void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient) {
    this.enforcementSdkClient = enforcementSdkClient;
  }
}
