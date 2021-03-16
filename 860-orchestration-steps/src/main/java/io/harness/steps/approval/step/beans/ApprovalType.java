package io.harness.steps.approval.step.beans;

import io.harness.EntitySubtype;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApprovalType implements EntitySubtype {
  HARNESS_APPROVAL("HarnessApproval");
  private final String displayName;

  ApprovalType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
