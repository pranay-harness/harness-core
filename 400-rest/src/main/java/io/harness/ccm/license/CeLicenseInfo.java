/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.license;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class CeLicenseInfo {
  @JsonIgnore public static final int CE_TRIAL_GRACE_PERIOD_DAYS = 15;

  private CeLicenseType licenseType;
  private long expiryTime;

  @JsonIgnore
  public long getExpiryTimeWithGracePeriod() {
    return expiryTime + Duration.ofDays(CE_TRIAL_GRACE_PERIOD_DAYS).toMillis();
  }

  @JsonIgnore
  public boolean isValidLicenceType() {
    switch (licenseType) {
      case LIMITED_TRIAL:
      case FULL_TRIAL:
      case PAID:
        return true;
      default:
        return false;
    }
  }
}
