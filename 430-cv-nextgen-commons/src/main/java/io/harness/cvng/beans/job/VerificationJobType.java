package io.harness.cvng.beans.job;

import java.util.Arrays;
import java.util.List;

public enum VerificationJobType {
  TEST,
  CANARY,
  BLUE_GREEN,
  HEALTH;

  public static List<VerificationJobType> getDeploymentJobTypes() {
    return Arrays.asList(TEST, CANARY, BLUE_GREEN);
  }
}
