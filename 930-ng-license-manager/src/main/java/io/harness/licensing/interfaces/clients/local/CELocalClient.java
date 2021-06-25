package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.LicenseConstant.UNLIMITED;
import static io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl.TRIAL_DURATION;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.stats.CERuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.CEModuleLicenseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CELocalClient implements CEModuleLicenseClient {
  @Override
  public CEModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CEModuleLicenseDTO.builder()
        .numberOfCluster(UNLIMITED)
        .spendLimit(Long.valueOf(UNLIMITED))
        .dataRetentionInDays(1825)
        .startTime(currentTime)
        .expiryTime(expiryTime)
        .build();
  }

  @Override
  public CERuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
