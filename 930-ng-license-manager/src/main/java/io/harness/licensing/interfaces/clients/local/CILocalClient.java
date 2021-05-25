package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.LicenseConstant.UNLIMITED;
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
  private static final int TRIAL_DEVELOPERS = 100;

  @Override
  public CIModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CIModuleLicenseDTO.builder()
        .maxDevelopers(UNLIMITED)
        .totalDevelopers(TRIAL_DEVELOPERS)
        .expiryTime(expiryTime)
        .transactions(Lists.newArrayList(CILicenseTransactionDTO.builder()
                                             .moduleType(ModuleType.CI)
                                             .licenseType(LicenseType.TRIAL)
                                             .accountIdentifier(accountId)
                                             .edition(edition)
                                             .developers(TRIAL_DEVELOPERS)
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
