package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl.TRIAL_DURATION;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.stats.CIRuntimeUsageDTO;
import io.harness.licensing.beans.transactions.CILicenseTransactionDTO;
import io.harness.licensing.interfaces.clients.CIModuleLicenseClient;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CILocalClient implements CIModuleLicenseClient {
  @Override
  public CIModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CIModuleLicenseDTO.builder()
        .maxDevelopers(10)
        .totalDevelopers(10)
        .transactions(Lists.newArrayList(CILicenseTransactionDTO.builder()
                                             .moduleType(ModuleType.CI)
                                             .licenseType(LicenseType.TRIAL)
                                             .accountIdentifier(accountId)
                                             .edition(edition)
                                             .developers(10)
                                             .startTime(currentTime)
                                             .expiryTime(expiryTime)
                                             .status(LicenseStatus.ACTIVE)
                                             .build()))
        .status(LicenseStatus.ACTIVE)
        .build();
  }

  @Override
  public CIRuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
