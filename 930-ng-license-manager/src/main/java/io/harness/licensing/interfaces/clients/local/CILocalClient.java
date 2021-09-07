package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.interfaces.ModuleLicenseImpl.TRIAL_DURATION;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.stats.CIRuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.CIModuleLicenseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CILocalClient implements CIModuleLicenseClient {
  private static final int TRIAL_DEVELOPERS = 100;

  @Override
  public CIModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CIModuleLicenseDTO.builder()
        .numberOfCommitters(TRIAL_DEVELOPERS)
        .startTime(currentTime)
        .expiryTime(expiryTime)

        .status(LicenseStatus.ACTIVE)
        .build();
  }

  @Override
  public CIRuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
