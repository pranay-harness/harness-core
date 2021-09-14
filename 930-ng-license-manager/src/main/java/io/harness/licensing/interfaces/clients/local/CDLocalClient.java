/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.interfaces.ModuleLicenseImpl.TRIAL_DURATION;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.types.CDLicenseType;
import io.harness.licensing.beans.stats.CDRuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.CDModuleLicenseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CDLocalClient implements CDModuleLicenseClient {
  private static final int TRIAL_WORKLOAD = 100;
  @Override
  public CDModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CDModuleLicenseDTO.builder()
        .cdLicenseType(CDLicenseType.SERVICES)
        .workloads(TRIAL_WORKLOAD)
        .startTime(currentTime)
        .expiryTime(expiryTime)
        .status(LicenseStatus.ACTIVE)
        .build();
  }

  @Override
  public CDRuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
