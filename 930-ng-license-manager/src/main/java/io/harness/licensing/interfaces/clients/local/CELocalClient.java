/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.interfaces.clients.local;

import static io.harness.licensing.LicenseConstant.UNLIMITED;
import static io.harness.licensing.interfaces.ModuleLicenseImpl.TRIAL_DURATION;

import io.harness.exception.UnsupportedOperationException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO.CEModuleLicenseDTOBuilder;
import io.harness.licensing.interfaces.clients.CEModuleLicenseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CELocalClient implements CEModuleLicenseClient {
  private static final long FREE_WORKLOAD = 250000;

  @Override
  public CEModuleLicenseDTO createTrialLicense(Edition edition, String accountId) {
    long expiryTime = Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli();
    long currentTime = Instant.now().toEpochMilli();

    CEModuleLicenseDTOBuilder<?, ?> builder =
        CEModuleLicenseDTO.builder().startTime(currentTime).expiryTime(expiryTime).status(LicenseStatus.ACTIVE);

    switch (edition) {
      case ENTERPRISE:
        return builder.spendLimit(Long.valueOf(UNLIMITED)).licenseType(LicenseType.TRIAL).build();
      case TEAM:
        return builder.spendLimit(Long.valueOf(UNLIMITED)).licenseType(LicenseType.TRIAL).build();
      case FREE:
        return builder.spendLimit(FREE_WORKLOAD).expiryTime(Long.MAX_VALUE).build();
      default:
        throw new UnsupportedOperationException("Requested edition is not supported");
    }
  }
}
