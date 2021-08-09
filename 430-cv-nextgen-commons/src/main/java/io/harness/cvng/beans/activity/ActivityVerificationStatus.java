package io.harness.cvng.beans.activity;

import java.util.Arrays;
import java.util.List;

public enum ActivityVerificationStatus {
  IGNORED,
  NOT_STARTED,
  VERIFICATION_PASSED,
  VERIFICATION_FAILED,
  ERROR,
  ABORTED,
  IN_PROGRESS;

  public static List<ActivityVerificationStatus> getFinalStates() {
    return Arrays.asList(ERROR, VERIFICATION_PASSED, VERIFICATION_FAILED, ABORTED);
  }
}
