package io.harness.beans;

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