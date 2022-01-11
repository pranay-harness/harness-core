/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.bases;

import io.harness.enforcement.beans.TimeUnit;
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
public class LicenseRateLimitRestriction
    extends Restriction implements EnforcementSdkSupportInterface, LicenseLimitInterface {
  String fieldName;
  String clientName;
  TimeUnit timeUnit;
  EnforcementSdkClient enforcementSdkClient;

  public LicenseRateLimitRestriction(
      RestrictionType restrictionType, String fieldName, TimeUnit timeUnit, EnforcementSdkClient enforcementSdkClient) {
    super(restrictionType);
    this.fieldName = fieldName;
    this.timeUnit = timeUnit;
    this.enforcementSdkClient = enforcementSdkClient;
  }

  @Override
  public void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient) {
    this.enforcementSdkClient = enforcementSdkClient;
  }
}
