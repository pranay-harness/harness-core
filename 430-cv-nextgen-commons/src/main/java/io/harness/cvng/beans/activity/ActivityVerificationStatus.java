/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
