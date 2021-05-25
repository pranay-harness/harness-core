package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl.TRIAL_DURATION;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.stats.CFRuntimUsageDTO;
import io.harness.licensing.beans.transactions.CFLicenseTransactionDTO;
import io.harness.licensing.interfaces.clients.CFModuleLicenseClient;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CFLocalClient implements CFModuleLicenseClient {
  @Override
  public CFModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CFModuleLicenseDTO.builder()
        .maxFeatureFlagUnit(3)
        .maxClientMAUs(25000L)
        .totalFeatureFlagUnit(3)
        .totalClientMAUs(25000L)
        .transactions(Lists.newArrayList(CFLicenseTransactionDTO.builder()
                                             .moduleType(ModuleType.CF)
                                             .licenseType(LicenseType.TRIAL)
                                             .accountIdentifier(accountId)
                                             .edition(edition)
                                             .featureFlagUnit(3)
                                             .clientMAU(25000L)
                                             .expiryTime(expiryTime)
                                             .startTime(currentTime)
                                             .status(LicenseStatus.ACTIVE)
                                             .build()))
        .status(LicenseStatus.ACTIVE)
        .build();
  }

  @Override
  public CFRuntimUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
