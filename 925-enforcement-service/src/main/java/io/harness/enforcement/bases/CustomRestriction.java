package io.harness.enforcement.bases;

import io.harness.enforcement.interfaces.EnforcementSdkSupportInterface;
import io.harness.enforcement.services.impl.EnforcementSdkClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomRestriction extends Restriction implements EnforcementSdkSupportInterface {
  String clientName;
  EnforcementSdkClient enforcementSdkClient;

  @Override
  public void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient) {
    this.enforcementSdkClient = enforcementSdkClient;
  }
}
