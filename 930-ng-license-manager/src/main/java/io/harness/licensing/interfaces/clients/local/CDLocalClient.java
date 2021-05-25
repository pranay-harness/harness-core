package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.LicenseConstant.UNLIMITED;
import static io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl.TRIAL_DURATION;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.stats.CDRuntimeUsageDTO;
import io.harness.licensing.beans.transactions.CDLicenseTransactionDTO;
import io.harness.licensing.interfaces.clients.CDModuleLicenseClient;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CDLocalClient implements CDModuleLicenseClient {
  @Override
  public CDModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CDModuleLicenseDTO.builder()
        .maxWorkLoads(UNLIMITED)
        .deploymentsPerDay(UNLIMITED)
        .totalWorkLoads(UNLIMITED)
        .transactions(Lists.newArrayList(CDLicenseTransactionDTO.builder()
                                             .workload(UNLIMITED)
                                             .moduleType(ModuleType.CD)
                                             .licenseType(LicenseType.TRIAL)
                                             .accountIdentifier(accountId)
                                             .edition(edition)
                                             .startTime(currentTime)
                                             .expiryTime(expiryTime)
                                             .status(LicenseStatus.ACTIVE)
                                             .build()))
        .status(LicenseStatus.ACTIVE)
        .build();
  }

  @Override
  public CDRuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
