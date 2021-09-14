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
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.stats.CFRuntimUsageDTO;
import io.harness.licensing.interfaces.clients.CFModuleLicenseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CFLocalClient implements CFModuleLicenseClient {
  private static final int TRIAL_FEATURE_FLAG_UNITS = 50;
  private static final long TRIAL_CLIENT_MAUS = 1000000;
  @Override
  public CFModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CFModuleLicenseDTO.builder()
        .numberOfUsers(TRIAL_FEATURE_FLAG_UNITS)
        .numberOfClientMAUs(TRIAL_CLIENT_MAUS)
        .startTime(currentTime)
        .expiryTime(expiryTime)

        .status(LicenseStatus.ACTIVE)
        .build();
  }

  @Override
  public CFRuntimUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
