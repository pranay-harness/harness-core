/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.alert.util;

import io.harness.cvng.beans.activity.ActivityVerificationStatus;

public enum VerificationStatus {
  VERIFICATION_PASSED("Verification Passed"),
  VERIFICATION_FAILED("Verification Failed");

  private String displayName;

  VerificationStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static VerificationStatus getVerificationStatus(ActivityVerificationStatus activityVerificationStatus) {
    VerificationStatus verificationStatus = null;

    if (activityVerificationStatus.equals(ActivityVerificationStatus.VERIFICATION_PASSED)) {
      verificationStatus = VerificationStatus.VERIFICATION_PASSED;
    }

    if (activityVerificationStatus.equals(ActivityVerificationStatus.VERIFICATION_FAILED)
        || activityVerificationStatus.equals(ActivityVerificationStatus.ERROR)) {
      verificationStatus = VerificationStatus.VERIFICATION_FAILED;
    }
    return verificationStatus;
  }
}
