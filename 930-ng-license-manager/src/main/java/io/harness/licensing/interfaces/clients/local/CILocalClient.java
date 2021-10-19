package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.LicenseConstant.UNLIMITED;
import static io.harness.licensing.interfaces.ModuleLicenseImpl.TRIAL_DURATION;

import io.harness.exception.UnsupportedOperationException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO.CIModuleLicenseDTOBuilder;
import io.harness.licensing.interfaces.clients.CIModuleLicenseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CILocalClient implements CIModuleLicenseClient {
  private static final int ENTERPRISE_TRIAL_DEVELOPERS = 200;
  private static final int TEAM_TRIAL_DEVELOPERS = 200;

  @Override
  public CIModuleLicenseDTO createTrialLicense(Edition edition, String accountId) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();

    CIModuleLicenseDTOBuilder<?, ?> builder =
        CIModuleLicenseDTO.builder().startTime(currentTime).expiryTime(expiryTime).status(LicenseStatus.ACTIVE);

    switch (edition) {
      case ENTERPRISE:
        return builder.numberOfCommitters(ENTERPRISE_TRIAL_DEVELOPERS).licenseType(LicenseType.TRIAL).build();
      case TEAM:
        return builder.numberOfCommitters(TEAM_TRIAL_DEVELOPERS).licenseType(LicenseType.TRIAL).build();
      case FREE:
        return builder.numberOfCommitters(Integer.valueOf(UNLIMITED)).expiryTime(Long.MAX_VALUE).build();
      default:
        throw new UnsupportedOperationException("Requested edition is not supported");
    }
  }
}
