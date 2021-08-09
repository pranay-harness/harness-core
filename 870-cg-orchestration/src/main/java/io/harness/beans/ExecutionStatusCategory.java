package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum ExecutionStatusCategory {
  SUCCEEDED("Succeeded"),
  ERROR("Error"),
  ACTIVE("Active");

  private String displayName;
  ExecutionStatusCategory(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}
