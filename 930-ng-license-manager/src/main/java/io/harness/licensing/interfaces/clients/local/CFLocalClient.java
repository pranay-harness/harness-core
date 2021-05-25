package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.LicenseConstant.UNLIMITED;
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
  private static final int TRIAL_FEATURE_FLAG_UNITS = 50;
  private static final long TRIAL_CLIENT_MAUS = 1000000;
  @Override
  public CFModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();
    return CFModuleLicenseDTO.builder()
        .maxFeatureFlagUnit(UNLIMITED)
        .maxClientMAUs(Long.valueOf(UNLIMITED))
        .totalFeatureFlagUnit(TRIAL_FEATURE_FLAG_UNITS)
        .totalClientMAUs(TRIAL_CLIENT_MAUS)
        .expiryTime(expiryTime)
        .transactions(Lists.newArrayList(CFLicenseTransactionDTO.builder()
                                             .moduleType(ModuleType.CF)
                                             .licenseType(LicenseType.TRIAL)
                                             .accountIdentifier(accountId)
                                             .edition(edition)
                                             .featureFlagUnit(TRIAL_FEATURE_FLAG_UNITS)
                                             .clientMAU(TRIAL_CLIENT_MAUS)
                                             .clientMAU(TRIAL_CLIENT_MAUS)
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
