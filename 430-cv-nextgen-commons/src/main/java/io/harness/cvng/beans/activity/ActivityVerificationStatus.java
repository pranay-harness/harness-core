package io.harness.cvng.beans.activity;

import java.util.Arrays;
import java.util.List;

public enum ActivityVerificationStatus {
  NOT_STARTED,
  VERIFICATION_PASSED,
  VERIFICATION_FAILED,
  ERROR,
  IN_PROGRESS;

  public static List<ActivityVerificationStatus> getFinalStates() {
    return Arrays.asList(ERROR, VERIFICATION_PASSED, VERIFICATION_FAILED);
  }
}
