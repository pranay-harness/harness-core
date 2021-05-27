package io.harness.pms.sdk.core;

public enum SdkDeployMode {
  LOCAL,
  REMOTE,
  REMOTE_IN_PROCESS;

  public boolean isNonLocal() {
    return this != LOCAL;
  }
}
